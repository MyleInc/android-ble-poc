package com.getmyle.mylesdk;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TapManager1 implements ServiceConnection {

    private static final int SIZE_QUEUE = 10;

    private Context mContext;
    private MyleService1 mMyleService;
    private boolean mBounded;
    private TapManagerListener mTapManagerListener;
    private BlockingQueue<byte[]> mQueue = new ArrayBlockingQueue<byte[]>(SIZE_QUEUE);
    private Thread mThread;

    BluetoothGatt mBluetoothGatt;

    public TapManager1(Context context) {
        mContext = context;
    }


    public Collection<BluetoothDevice> getAvailableTaps() {
        return mMyleService.mAvailableTaps.values();
    }

    public String getTapName(String address) {
        return mMyleService.mAvailableTapNames.get(address);
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
    public void setMyleServiceListener(MyleService1.MyleServiceListener listener) {
        mMyleService.addMyleServiceListener(listener);
    }

    /**
     * Need to call in onServiceConnected().
     *
     * @param listener This listener will be used for reading data from devices.
     */
    public void setParameterListener(MyleService1.ParameterListener listener) {
        mMyleService.addParameterListener(listener);
    }

    /**
     * Need to call in onServiceDisconnected() to delete the listener.
     * This method should be used as pair with onServiceConnected().
     *
     * @param listener This listener will be deleted.
     */
    public void removeMyleServiceListener(MyleService1.MyleServiceListener listener) {
        mMyleService.removeMyleServiceListener(listener);
    }

    /**
     * This method should be used as pair with setParameterListener().
     *
     * @param listener This listener will be deleted.
     */
    public void removeParameterListener(MyleService1.ParameterListener listener) {
        mMyleService.removeParameterListener(listener);
    }

    /**
    * Need to call in Activity#onCreate() or Activity#onStart() or Activity#onResume().
    * If connection is OK, the method onServiceConnected() will be called.
    */
    public void setup() {
        Intent intent = new Intent(mContext, MyleService1.class);
        mContext.startService(intent);
        mContext.bindService(intent, this, 0);
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
        //mMyleService.connect(address, password);
        BluetoothDevice tap = mMyleService.mAvailableTaps.get(address);
        if (tap == null) {
            // throw exception?
            return;
        }

        mBluetoothGatt = tap.connectGatt(mContext, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {

                        //if (isConnected()) return;

                       // Log.i(TAG, "Connected");

                        // Set LE to high speed mode
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        }

                        //mIsConnected = true;
                        //mBLEWrapperListener.onConnectResult(Constant.ConnectState.BLE_CONNECT_SUCCESS, "null");
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED/* && mIsConnected*/) {
                        //Log.i(TAG, "disconnect");
                        //mIsConnected = false;
                        //mBLEWrapperListener.onDisconnected();
                    }
                } else { /* connect fail */
                    //Log.i(TAG, "Gatt error = " + status);
                    //mBLEWrapperListener.onConnectResult(Constant.ConnectState.BLE_CONNECT_FAIL, status + "");
                }
                //super.onConnectionStateChange(gatt, status, newState);
            }
        });
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
        MyleService1.LocalBinder mLocalBinder = (MyleService1.LocalBinder) service;
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
