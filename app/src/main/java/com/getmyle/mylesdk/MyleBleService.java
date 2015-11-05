package com.getmyle.mylesdk;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.SyncStateContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import java.util.Arrays;

/**
 * Created by mikalai on 2015-11-04.
 */
public class MyleBleService extends Service {

    private static final String TAG = MyleBleService.class.getSimpleName();

    private final IBinder binder = new LocalBinder();

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothGatt btGatt;

    private HashMap<String, BluetoothDevice> availableTaps = new HashMap<>(10);
    private HashMap<String, String> availableTapNames = new HashMap<>(10);

    private boolean isScanning = false;

    private BluetoothGattCharacteristic writeChrt;
    private BluetoothGattCharacteristic readChrt;

    private FileReceiver fileReceiver = new FileReceiver();


    @Override
    public void onCreate() {
        super.onCreate();

        this.btManager = (BluetoothManager) this.getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        this.btAdapter = this.btManager.getAdapter();

        this.startScan();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // this is needed to bind TapManager to this service,
        // so TapManager has access to current instance
        return binder;
    }


    public class LocalBinder extends Binder {
        public MyleBleService getServerInstance() {
            return MyleBleService.this;
        }
    }


    public void startScan() {
        this.availableTaps.clear();
        this.availableTapNames.clear();

        BluetoothLeScanner scanner = this.btAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().build();
        List<ScanFilter> filters = new ArrayList<>();

        // scan for TAPs
        scanner.startScan(filters, settings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // filtering by service UUID above doesn't work
                // so we use trick from http://stackoverflow.com/a/24539704/444630
                // to extract service UUID
                List<UUID> uuids = Utils.parseUuidsByAdvertisedData(result.getScanRecord().getBytes());
                if (!uuids.contains(Constant.SERVICE_UUID)) {
                    return;
                }

                String address = result.getDevice().getAddress();
                if (availableTaps.containsKey(address)) {
                    return;
                }

                String name = Utils.getNameByScanRecord(result.getScanRecord().getBytes());
                Log.i(TAG, "Found tap " + name + " " + address);

                availableTaps.put(address, result.getDevice());
                availableTapNames.put(address, name);

                Intent intent = new Intent(Constant.TAP_NOTIFICATION_SCAN);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.i(TAG, "Scan failed with errorCode" + errorCode);
            }
        });

        this.isScanning = true;
        Log.i(TAG, "Scan started...");
    }


    public void stopScan() {
        BluetoothLeScanner scanner = this.btAdapter.getBluetoothLeScanner();
        scanner.stopScan(new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                Log.i(TAG, "Scan failed with errorCode" + errorCode);
            }
        });

        this.isScanning = false;
        Log.i(TAG, "Scan stopped");
    }


    public boolean isScanning() {
        return this.isScanning;
    }


    public Collection<BluetoothDevice> getAvailableTaps() {
        return this.availableTaps.values();
    }


    public String getTapName(String address) {
        return this.availableTapNames.get(address);
    }


    public void connectToTap(String address, final String password) {
        BluetoothDevice tap = this.availableTaps.get(address);
        if (tap == null) {
            // throw exception?
            return;
        }

        this.stopScan();

        // NOTE: we want autoConnect parameter of connectGatt() to be true
        // because for some reason during recording tap is getting disconnected with status 19
        // so we would like it to be auto-connected once recording is done
        boolean autoConnect = true;
        this.btGatt = tap.connectGatt(this.getApplication(), autoConnect, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                // NOTE: there is an interesting case when a tap was connected previously
                // and is currently recording something, it's getting disconnected with status 19.
                // Once recorded the tap is reconnected back thanks to autoConnect parameter of connectGatt() above.
                // So we decided not to consider this as an error.
                boolean disconnectedForRecording = (status == 19 && newState == BluetoothProfile.STATE_DISCONNECTED);

                if (status == BluetoothGatt.GATT_SUCCESS || disconnectedForRecording) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to tap " + gatt.getDevice().getAddress());

                        btGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                        btGatt.discoverServices();

                        Intent intent = new Intent(Constant.TAP_NOTIFICATION_TAP_CONNECTED);
                        intent.putExtra(Constant.TAP_NOTIFICATION_TAP_CONNECTED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from to tap " + gatt.getDevice().getAddress());

                        Intent intent = new Intent(Constant.TAP_NOTIFICATION_TAP_DISCONNECTED);
                        intent.putExtra(Constant.TAP_NOTIFICATION_TAP_DISCONNECTED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    }
                } else {
                    Log.i(TAG, "Connection failed with status " + status);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Failed discovering services with status " + status);
                    return;
                }
                Log.i(TAG, "Discovered services");

                // adjust our WRITE characteristic
                writeChrt = gatt
                        .getService(Constant.SERVICE_UUID)
                        .getCharacteristic(Constant.CHARACTERISTIC_UUID_TO_WRITE);
                writeChrt.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                // enable notifications for our READ characteristic
                readChrt = gatt
                        .getService(Constant.SERVICE_UUID)
                        .getCharacteristic(Constant.CHARACTERISTIC_UUID_TO_READ);
                gatt.setCharacteristicNotification(readChrt, true);

                BluetoothGattDescriptor descriptor = readChrt.getDescriptor(UUID.fromString(Constant.NOTIFICATION_DESCRIPTOR));
                if (descriptor != null) {
                    byte[] val = true ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                    descriptor.setValue(val);
                    gatt.writeDescriptor(descriptor);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] value = characteristic.getValue();

                if (fileReceiver.isInProgress()) {
                    fileReceiver.append(value);
                } else if (Utils.startsWith(value, Constant.MESSAGE_CONNECTED)) {
                    // tap accepted password - continue with time sync
                    sendTime();
                } else if (Utils.startsWith(value, Constant.MESSAGE_FILE_AUDIO)) {
                    Log.i(TAG, "Start receiving audio file...");
                    fileReceiver.start(new FileReceiver.Callbacks() {
                        @Override
                        public void acknowledge(byte[] ack) {
                            send(ack);
                        }

                        @Override
                        public void onComplete(Date time, byte[] buffer, int speed) {
                            Log.i(TAG, "Received audio file recorded at " + time + " with size " + buffer.length + " bytes at speed " + speed);
                        }
                    });
                } else if (Utils.startsWith(value, Constant.MESSAGE_FILE_LOG)) {
                    Log.i(TAG, "Start receiving log file...");
                    fileReceiver.start(new FileReceiver.Callbacks() {
                        @Override
                        public void acknowledge(byte[] ack) {
                            send(ack);
                        }

                        @Override
                        public void onComplete(Date time, byte[] buffer, int speed) {
                            Log.i(TAG, "Received log file recorded at " + time + " with size " + buffer.length + " bytes at speed " + speed);
                        }
                    });
                } else {
                    Log.i(TAG, "onCharacteristicChanged Unhandled value of " + characteristic.getUuid() + ": " + characteristic.getStringValue(0));
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                //super.onCharacteristicRead(gatt, characteristic, status);
                Log.i(TAG, "onCharacteristicRead " + characteristic.getUuid() + " with status " + status + ": " + characteristic.getStringValue(0));
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                //super.onCharacteristicWrite(gatt, characteristic, status);
                //Log.i(TAG, "onCharacteristicWrite " + characteristic.getUuid() + " with status " + status + ": " + characteristic.getStringValue(0));
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                //super.onDescriptorRead(gatt, descriptor, status);
                Log.i(TAG, "onDescriptorRead " + descriptor.getUuid() + " with status " + status + ": " + descriptor.getValue());
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                //super.onDescriptorWrite(gatt, descriptor, status);
                Log.i(TAG, "onDescriptorWrite " + descriptor.getUuid() + " with status " + status + ": " + descriptor.getValue());

                // NOTE: for some reason we send password once descriptor for notification was written
                // send password to tap and wait until confirmation comes back (CONNECTED value in read chrt)
                sendPassword(password);
            }
        });

        Log.i(TAG, "Connecting to tap " + address + "...");
    }


    /**
     * Send password to tap
     */
    private void sendPassword(String password) {

        byte data1[] = new byte[]{'5', '5', '0', '1', (byte) password.length()};
        byte data2[] = password.getBytes();
        byte data3[] = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);

        send(data3);

        Log.i(TAG, "Sent password");
    }


    private void sendTime() {
        Calendar c = Calendar.getInstance();

        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        byte second = (byte) c.get(Calendar.SECOND);
        byte minute = (byte) c.get(Calendar.MINUTE);
        byte hour = (byte) c.get(Calendar.HOUR_OF_DAY);
        byte date = (byte) c.get(Calendar.DAY_OF_MONTH);
        byte month = (byte) (c.get(Calendar.MONTH));
        byte year = (byte) (c.get(Calendar.YEAR) - 2000);

        byte[] timeData = new byte[]{'5', '5', '0', '0', second, minute, hour, date, month, year};

        send(timeData);

        Log.i(TAG, "Sent UTC time " + Arrays.toString(timeData));
    }


    public void send(byte[] data) {
        this.writeChrt.setValue(data);
        this.btGatt.writeCharacteristic(this.writeChrt);
    }


}
