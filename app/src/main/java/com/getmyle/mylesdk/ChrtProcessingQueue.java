package com.getmyle.mylesdk;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by mikalai on 2015-11-05.
 */
public class ChrtProcessingQueue {

    ArrayBlockingQueue<QueueItem> toProcess = new ArrayBlockingQueue<>(1000);
    Semaphore nextItemLock = new Semaphore(0);
    BluetoothGatt gatt;

    public ChrtProcessingQueue(BluetoothGatt gatt) {
        this.gatt = gatt;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // wait until there is anything in toProcess queue and process when available
                        process(toProcess.take());

                        // wait until the item is processed
                        nextItemLock.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    /**
     * Queues write characteristic action.
     * @param chrt
     * @param writeData
     */
    public void put(BluetoothGattCharacteristic chrt, byte[] writeData) {
        try {
            this.toProcess.put(new QueueItem(chrt, writeData));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Queues read characteristic action.
     * @param chrt
     */
    public void put(BluetoothGattCharacteristic chrt) {
        try {
            this.toProcess.put(new QueueItem(chrt));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Is called to indicate that current item is processed, so next one can be handled.
     */
    public void processNext() {
        // release next item lock
        nextItemLock.release();
    }


    /**
     * Pprocesses given action.
     * @param item
     */
    private void process(QueueItem item) {
        if (item.isReading()) {
            this.gatt.readCharacteristic(item.chrt);
        } else {
            item.chrt.setValue(item.writeData);
            gatt.writeCharacteristic(item.chrt);
        }
    }


    private class QueueItem {
        public BluetoothGattCharacteristic chrt;
        public byte[] writeData;

        public QueueItem(BluetoothGattCharacteristic chrt, byte[] writeData) {
            this.chrt = chrt;
            this.writeData = writeData;
        }

        public QueueItem(BluetoothGattCharacteristic chrt) {
            this.chrt = chrt;
        }

        public boolean isReading() {
            return this.writeData == null;
        }
    }
}
