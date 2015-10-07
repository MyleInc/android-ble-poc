package com.getmyle.mylesdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/*
 * BLE implementation.
 * 
 * @author: Ideas&Solutions
 * @date: 03/29/2015
 */

public class BleWrapper {
    private static final String TAG = "BleWrapper";
    private BLEWrapperListener mBLEWrapperListener;
    private Context mContext;
    private boolean mIsConnected;
    private boolean mScanning;
    private String mConnectingDeviceAddress;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattService> mBluetoothGattServices = new ArrayList<BluetoothGattService>();
    private Queue<byte[]> mWriteCharacQueue = new LinkedList<byte[]>();
    private boolean subscribeToBatLevel = false;

    public BleWrapper(Context context) {
        this.mContext = context;

        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    public void finalize() {
        mBluetoothAdapter = null;
        mBluetoothManager = null;
    }

    public void setBLEWrapperListener(BLEWrapperListener listener) {
        this.mBLEWrapperListener = listener;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    /**
     * Check if this android device supports BLE or not.
     */
    public boolean checkBleHardwareAvailable() {
        boolean hasBle;
        hasBle = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        return hasBle;
    }


    /**
     * Check if device has bluetooth is enable or not.
     */
    public boolean isBtEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Turn on android device's blutooth
     */
    public void enableBt() {
        mBluetoothAdapter.enable();
    }

    /**
     * Configure to reduce connection interval
     */
    public void configureHighSpeedMode() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
    }

    /**
     * Start bluetooth scan.
     */
    public synchronized void startScanning() {
        if (mScanning) {
            return;
        }

        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
        mScanning = true;
    }

    /**
     * Stop bluetooth scan
     */
    public synchronized void stopScanning() {
        if (mScanning) {
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);
                mScanning = false;
            }
        }
    }

    /**
     * Connect to the device with specified address
     */
    public void connect(String deviceAddress) {
        // Previously connected device.  Try to reconnect.
//        if (mConnectingDeviceAddress != null && deviceAddress.equals(mConnectingDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) return;
//        }

        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Method connectGattMethod = null;

            try {
                connectGattMethod = mBluetoothDevice.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            try {
                mBluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(mBluetoothDevice, mContext, true, mBleCallback, 2);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, true, mBleCallback);
        }

        // Save connecting device address for reconnect later
        mConnectingDeviceAddress = deviceAddress;
    }

    /**
     * Disconnect the device.
     * It is still possible to reconnect to it later with this Gatt client.
     */
    public synchronized void diconnect() {
        if (null == mBluetoothGatt) return;

        mIsConnected = false;
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        subscribeToBatLevel = false;
    }

    /**
     * Close GATT client completely
     */
    public void close() {
        if (null == mBluetoothGatt) return;

        mBluetoothGatt.close();
        mBluetoothGatt = null;
        subscribeToBatLevel = false;
    }

    /**
     * Request to discover all services available on the remote devices
     * results are delivered through callback object
     */
    public void startServicesDiscovery() {
        if (null != mBluetoothGatt) {
            mBluetoothGatt.discoverServices();
        }
    }

    /**
     * Gets services, make sure service discovery is finished!
     */
    public void getSupportedServices() {
        mBluetoothGattServices.clear();

        if (mBluetoothGatt != null) {
            mBluetoothGattServices = mBluetoothGatt.getServices();
            mBLEWrapperListener.onFoundService(mBluetoothGattServices);
        }
    }

    /**
     * Get all characteristic for particular service.
     */
    public void getCharacteristicsForService(BluetoothGattService service) {
        if (null == service) return;

        List<BluetoothGattCharacteristic> chars = null;
        chars = service.getCharacteristics();
        mBLEWrapperListener.onListCharacteristics(chars);
    }

    /**
     * Write to particular characteristic
     */
    public void writeDataToCharacteristic(BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
        if (null == ch) return;

        mWriteCharacQueue.add(dataToWrite);

        if (mWriteCharacQueue.size() == 1) {
            ch.setValue(mWriteCharacQueue.element());
            mBluetoothGatt.writeCharacteristic(ch);
        }
    }

    /**
     * Get connected device
     */
    public BluetoothDevice getConnectedDevice() {
        List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

        if (devices.size() > 0) {
            return devices.get(0);
        }

        return null;
    }

    /**
     * Request to fetch newest value stored on the remote device for particular characteristic
     */
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        mBluetoothGatt.readCharacteristic(ch);
    }

    /**
     * enables/disables notification for characteristic
     */
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if (!success) {
            Log.e("------", "Seting proper notification status for characteristic failed!");
        }

        // This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
        // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString(Constant.NOTIFICATION_DESCRIPTOR));
        if (descriptor != null) {
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        Log.i(TAG, "descriptor = " + descriptor.getUuid().toString());
    }

    /**
     * Callback for scanning has new device
     */
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            mBLEWrapperListener.onFoundDevice(device, rssi, scanRecord);
        }
    };

    /**
     * Callbacks called for any action on particular Ble Device
     */
    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (isConnected()) return;

                    Log.i(TAG, "Connected");

                    // Set LE to high speed mode
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                    }

                    mIsConnected = true;
                    mBLEWrapperListener.onConnectResult(Constant.ConnectState.BLE_CONNECT_SUCCESS, "null");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED && mIsConnected) {
                    Log.i(TAG, "disconnect");
                    mIsConnected = false;
                    mBLEWrapperListener.onDisconnected();
                }
            } else { /* connect fail */
                Log.i(TAG, "Gatt error = " + status);
                mBLEWrapperListener.onConnectResult(Constant.ConnectState.BLE_CONNECT_FAIL, status + "");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // now, when services discovery is finished, we can call getServices() for Gatt
                getSupportedServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(TAG, "Read done");
            if (characteristic.getUuid().toString().equals(Constant.BATTERY_LEVEL_UUID)) {
                Log.i(TAG, "onCharacteristicRead, Battery level");
                mBLEWrapperListener.onReadBatteryLevel(gatt,
                        characteristic,
                        characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().toString().equals(Constant.CHARACTERISTIC_UUID_TO_READ)) {

                Log.i(TAG, "onCharacteristicChanged");
                mBLEWrapperListener.onReceiveData(gatt,
                        characteristic,
                        characteristic.getValue());
/*
                if(subscribeToBatLevel == false){
                    subscribeToBatLevel = true;
                    mBLEWrapperListener.setNotificationForCharacteristicBattery(gatt.getService(UUID.fromString(Constant.BATTERY_SERVICE_UUID)).getCharacteristic(UUID.fromString(Constant.BATTERY_LEVEL_UUID)), subscribeToBatLevel);
                }
 */
            }else
            if (characteristic.getUuid().toString().equals(Constant.BATTERY_LEVEL_UUID)) {
                Log.i(TAG, "onCharacteristicChanged, Battery level");
                mBLEWrapperListener.onReceiveBatteryLevel(gatt,
                        characteristic,
                        characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mWriteCharacQueue.remove();

                if (mWriteCharacQueue.size() > 0) {
                    characteristic.setValue(mWriteCharacQueue.element());
                    mBluetoothGatt.writeCharacteristic(characteristic);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite, status = " + status);
            Log.i(TAG, "descriptor = " + descriptor.getUuid().toString());

            mBLEWrapperListener.onDidConfigureGatt();
        }
    };

    interface BLEWrapperListener {
        public void onFoundDevice(BluetoothDevice device, final int rssi, final byte[] scanRecord);

        public void onConnectResult(Constant.ConnectState resultCode, String error);

        public void onDisconnected();

        public void onFoundService(List<BluetoothGattService> service);

        public void onListCharacteristics(List<BluetoothGattCharacteristic> chars);

        public void onReceiveData(BluetoothGatt gatt, BluetoothGattCharacteristic charac, byte[] data);

        public void setNotificationForCharacteristicBattery(BluetoothGattCharacteristic charac, boolean enabled);

        public void onReceiveBatteryLevel(BluetoothGatt gatt, BluetoothGattCharacteristic charac, byte[] data);
        public void onReadBatteryLevel(BluetoothGatt gatt, BluetoothGattCharacteristic charac, byte[] data);

        public void onDidConfigureGatt(); /* Config charecterestic to nofity charac change */
    }
}
