package com.getmyle.mylesdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TapManager implements ServiceConnection {

    private static final int SIZE_QUEUE = 10;

    private Context mContext;
    private MyleService mMyleService;
    private boolean mBounded;
    private TapManagerListener mTapManagerListener;
    private BlockingQueue<byte[]> mQueue = new ArrayBlockingQueue<byte[]>(SIZE_QUEUE);
    private Thread mThread;

    public TapManager(Context context) {
        mContext = context;
    }

    /**
     * Need to call after creating an object of type TapManager
     */
    public void setTapManagerListener(TapManagerListener listener) {
        mTapManagerListener = listener;
    }

    /**
    * Need to call in onServiceConnected() and setup listener.
    *
    * @param listener This listener will be used for getting founded devices and logging actions.
    */
    public void setMyleServiceListener(MyleService.MyleServiceListener listener) {
        mMyleService.addMyleServiceListener(listener);
    }

    /**
     * Need to call in onServiceConnected().
     *
     * @param listener This listener will be used for reading data from devices.
     */
    public void setParameterListener(MyleService.ParameterListener listener) {
        mMyleService.addParameterListener(listener);
    }

    /**
     * Need to call in onServiceDisconnected() to delete the listener.
     * This method should be used as pair with onServiceConnected().
     *
     * @param listener This listener will be deleted.
     */
    public void removeMyleServiceListener(MyleService.MyleServiceListener listener) {
        mMyleService.removeMyleServiceListener(listener);
    }

    /**
     * This method should be used as pair with setParameterListener().
     *
     * @param listener This listener will be deleted.
     */
    public void removeParameterListener(MyleService.ParameterListener listener) {
        mMyleService.removeParameterListener(listener);
    }

    /**
    * Need to call in Activity#onCreate() or Activity#onStart() or Activity#onResume().
    * If connection is OK, the method onServiceConnected() will be called.
    */
    public void connectToService() {
        Intent intent = new Intent(mContext, MyleService.class);
        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    /**
     * Need to call in Activity#onPause() or Activity#onStop() or Activity#onDestroy()
     */
    public void destroy() {
        mQueue.clear();

        mMyleService.disconnect();

        // Unbound service
        if (mBounded) {
            mContext.unbindService(this);
            mBounded = false;
        }

        mTapManagerListener = null;
    }

    public void connectToDevice(String address, String password) {
        mMyleService.connect(address, password);
    }

    public void forgetCurrentDevice() {
        mMyleService.forgetCurrentDevice();
    }

    public int getReceiveByteAudio() {
        return mMyleService.getReceiveByteAudio();
    }

    public void stopScan() {
        mMyleService.stopScan();
    }

    public void startScan() {
        mMyleService.startScan();
    }

    public void sendWriteRECLN(String value) {
        String temp = String.format(Locale.getDefault(), "%02d", Integer.parseInt(value));
        String str = "5502RECLN" + temp;
        Log.i("", str);

        processRequests(str.getBytes());
    }

    public void sendWritePAUSELEVEL(String value) {
        String temp = String.format(Locale.getDefault(), "%03d", Integer.parseInt(value));
        String str = "5502PAUSELEVEL" + temp;

        processRequests(str.getBytes());
    }

    public void sendWritePAUSELEN(String value) {
        String temp = String.format(Locale.getDefault(), "%02d", Integer.parseInt(value));
        String str = "5502PAUSELEN" + temp;

        processRequests(str.getBytes());
    }

    public void sendWriteACCELERSENS(String value) {
        String temp = String.format(Locale.getDefault(), "%03d", Integer.parseInt(value));
        String str = "5502ACCELERSENS" + temp;

        processRequests(str.getBytes());
    }

    public void sendWriteMIC(String value) {
        String temp = String.format(Locale.getDefault(), "%03d", Integer.parseInt(value));
        String str = "5502MIC" + temp;

        processRequests(str.getBytes());
    }

    public void sendWriteBTLOC(String value) {
        String temp = "5502BTLOC" + value;

        processRequests(temp.getBytes());
    }

    public void sendWritePASSWORD(String value) {
        byte[] a = new byte[]{'5', '5', '0', '2', 'P', 'A', 'S', 'S', (byte) value.length()};
        byte[] b = value.getBytes();

        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        processRequests(c);
    }

    public void sendReadRECLN() {
        processRequests("5503RECLN".getBytes());
    }

    public void sendReadBTLOC() {
        processRequests("5503BTLOC".getBytes());
    }

    public void sendReadPAUSELEVEL() {
        processRequests("5503PAUSELEVEL".getBytes());
    }

    public void sendReadPAUSELEN() {
        processRequests("5503PAUSELEN".getBytes());
    }

    public void sendReadACCELERSENS() {
        processRequests("5503ACCELERSENS".getBytes());
    }

    public void sendReadMIC() {
        processRequests("5503MIC".getBytes());
    }

    public void sendReadVERSION() {
        processRequests("5503VERSION".getBytes());
    }

    public void sendReadBATTERY_LEVEL() {
        mMyleService.requestBatteryLevelValue();
    }

    public void sendEnableBatteryNotification(){
        mMyleService.enableBatteryLevelNotification();
    }

    // Not public interface
    public void onServiceDisconnected(ComponentName name) {
        if (mTapManagerListener != null) {
            mTapManagerListener.onServiceDisconnected();
        }

        mBounded = false;
        mMyleService = null;
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        mBounded = true;
        MyleService.LocalBinder mLocalBinder = (MyleService.LocalBinder) service;
        mMyleService = mLocalBinder.getServerInstance();

        if (mTapManagerListener != null) {
            mTapManagerListener.onServiceConnected();
        }
    }

    // Helper methods
    //
    // This method supports a queue of requests and runs each request after some delay
    private void processRequests(byte[] request) {
        if (mQueue.size() == SIZE_QUEUE) {
            return;
        }

        mQueue.add(request);

        if ((mThread == null) || (!mThread.isAlive())) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Is there any elements in the queue?
                    while (mQueue.peek() != null) {
                        // Take an element and process it
                        try {
                            mMyleService.send(mQueue.take());

                            // This delay need to send sequences of requests
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });

            mThread.start();
        }
    }

    public interface TapManagerListener {
        void onServiceConnected();

        void onServiceDisconnected();
    }

}
