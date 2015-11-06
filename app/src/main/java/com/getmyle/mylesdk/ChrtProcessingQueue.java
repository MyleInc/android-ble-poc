package com.getmyle.mylesdk;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by mikalai on 2015-11-05.
 */
public class ChrtProcessingQueue {

    ArrayBlockingQueue<QueueItem> toProcess = new ArrayBlockingQueue<>(10000);
    ArrayBlockingQueue<QueueItem> processingQueue = new ArrayBlockingQueue<>(1);
    BluetoothGatt gatt;

    QueueItem currentItem;

    public ChrtProcessingQueue(BluetoothGatt gatt) {
        this.gatt = gatt;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        currentItem = toProcess.take();
                        process(currentItem);
                        processingQueue.take();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }

    public void put(BluetoothGattCharacteristic chrt, byte[] writeData) {
        try {
            this.toProcess.put(new QueueItem(chrt, writeData));
        } catch (InterruptedException e) {
        }
    }

    public void put(BluetoothGattCharacteristic chrt) {
        try {
            this.toProcess.put(new QueueItem(chrt));
        } catch (InterruptedException e) {
        }
    }


    /**
     * Removes head from queue and processes next request.
     */
    public void processNext() {
        try {
            processingQueue.put(currentItem);
        } catch (InterruptedException e) {
        }
    }

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
