package com.getmyle.mylesdk;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by mikalai on 2015-11-04.
 */
public class FileReceiver {

    // whetere we ar ein the middle of file transfer
    private boolean isInProgress = false;

    // registered callbacks
    private Callbacks callbacks;

    // Size of package after which we have toc acknowledge received data
    // NOTE: BLE transfers data in smaller pieces (20 bytes?), "package" is just logical assumption
    private static final int PACKAGE_SIZE = 282;//423;

    // File length to receive
    private int fileLength = 0;

    // file date from metadata received
    private Date fileDate;

    // file data
    private byte[] fileBuffer;

    // Time when we started file transfer
    private long startTime = 0;

    // this is to track number of bytes for a package
    // NOTE: we need to track that, because we need to acknowledge to
    // tap at the end of whole package
    private int numBytesReceivedWithinPackage = 0;

    // this is to track number of bytes for whole file
    private int numBytesReceived = 0;


    /**
     * Whether a file is being received.
     *
     * @return true if it is, false otherwise
     */
    public boolean isInProgress() {
        return this.isInProgress;
    }


    /**
     * Starts file receiving.
     *
     * @param callbacks are called to interact with caller about some events
     */
    public void start(Callbacks callbacks) {
        this.callbacks = callbacks;
        this.isInProgress = true;
        this.fileLength = 0;
    }


    /**
     * Appends new bunch of data.
     *
     * @param data
     */
    public void append(byte[] data) {
        if (this.fileLength == 0) {
            this.fileLength = getFileLength(data);
            this.fileDate = getFileCreationTime(data);
            this.fileBuffer = new byte[this.fileLength];
            this.numBytesReceived = 0;
            this.numBytesReceivedWithinPackage = 0;

            if (fileLength == 0) {
                complete(0);
                return;
            }

            this.startTime = System.nanoTime();

            acknowledge(PACKAGE_SIZE);
        } else if (numBytesReceived < fileLength) {
            // copy data to our buffer
            System.arraycopy(data, 0, this.fileBuffer, numBytesReceived, data.length);
            numBytesReceived += data.length;

            if (numBytesReceived >= fileLength) {
                long transferTime = System.nanoTime() - this.startTime;

                complete((int) (fileLength / (transferTime / 1000000000.0)));
            } else {
                numBytesReceivedWithinPackage += data.length;

                int nextPackageLength = (fileLength - numBytesReceived < PACKAGE_SIZE)
                        ? fileLength - numBytesReceived
                        : PACKAGE_SIZE;

                if (numBytesReceivedWithinPackage >= nextPackageLength) {
                    numBytesReceivedWithinPackage = 0;
                    acknowledge(nextPackageLength);
                }
            }
        }
    }


    /**
     * Extracts file size from metadata received
     *
     * @param data
     * @return
     */
    private int getFileLength(byte[] data) {
        // Get audio length. Byte 4 to byte 7
        return (int) ((data[4] & 0xff) + (data[5] & 0xff) * (Math.pow(16, 2)) +
                (data[6] & 0xff) * (Math.pow(16, 4)) + (data[7] & 0xff) * (Math.pow(16, 6)));
    }


    /**
     * Extracts file time from metadata received
     *
     * @param data
     * @return
     */
    private Date getFileCreationTime(byte[] data) {
        // Get date time. byte 8 to 11
        int temp = (int) (((data[8]) & 0xff) + ((data[9]) & 0xff) * (Math.pow(16, 2)) +
                ((data[10]) & 0xff) * (Math.pow(16, 4)) + ((data[11]) & 0xff) * (Math.pow(16, 6)));

        int second = ((temp & 0x1f) * 2);
        temp = temp >> 5;

        int min = temp & 0x3f;
        temp = temp >> 6;

        int hour = temp & 0x1f;
        temp = temp >> 5;

        int day = temp & 0x1f;
        temp = temp >> 5;

        int month = temp & 0xf;
        temp = temp >> 4;

        int year = (temp & 0x7f) + 1980;

        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        c.set(year, month, day, hour, min, second);

        return c.getTime();
    }


    /**
     * Notifies caller that acknowledge has to be made about received data.
     *
     * @param numBytes
     */
    private void acknowledge(int numBytes) {
        byte[] ack = new byte[]{(byte) (numBytes & 0xff), (byte) (numBytes >> 8)};
        this.callbacks.acknowledge(ack);
    }


    /**
     * Notifies caller about completion of file receiving.
     *
     * @param speed
     */
    private void complete(int speed) {
        this.isInProgress = false;
        this.callbacks.onComplete(this.fileDate, this.fileBuffer, speed);
    }


    public static abstract class Callbacks {
        /**
         * Is called when file receiving is completed.
         *
         * @param date   time when file was created on tap
         * @param buffer file buffer
         * @param speed  transfer speed
         */
        public void onComplete(Date date, byte[] buffer, int speed) {
        }


        /**
         * Is called to acknowledge package receiving.
         *
         * @param ack confirmation
         */
        public void acknowledge(byte[] ack) {
        }
    }

}
