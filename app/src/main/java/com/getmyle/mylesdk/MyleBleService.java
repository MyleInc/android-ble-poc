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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import java.util.Arrays;

/**
 * Created by mikalai on 2015-11-04.
 */
public class MyleBleService extends Service {

    public static final String INTENT_PARAM_INIT_ADDRESS = "initAddress";
    public static final String INTENT_PARAM_INIT_PASSWORD = "initPassword";

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
    private BluetoothGattCharacteristic batteryChrt;

    // the following fields are used to store
    // current device info, so we can reconnect next
    // time we see device automatically
    private String currentTapAddress;
    private String currentTapPassword;

    // a component that encapsulates file receiving logic
    private FileReceiver fileReceiver = new FileReceiver();

    // Android has stupid API for BLE.
    // It can happen that during writing subsequent values into a characteristic
    // some of them won't be processed. Even reading and writing characteristic
    // at the same time can conflict.
    // That's why we implement our own queue, to handle that.
    // NOTE: file receiving writes go directly to characteristic to speed things up.
    private ChrtProcessingQueue chrtProcessingQueue;

    // a flag to track period when password is send and response received.
    // Response can have two types:
    // 1. if password is good, "CONNECTED" message is returned
    // 2, if password is bad, tap disconnects for phone
    private boolean isAuthenticating = false;

    // listeners
    private LinkedList<TapManager.CharacteristicValueListener> characteristicValueListeners = new LinkedList<>();
    private LinkedList<TapManager.TraceListener> traceListeners = new LinkedList<>();



    @Override
    public void onCreate() {
        super.onCreate();

        // default trace listener logs everything
        this.addTraceListener(new TapManager.TraceListener() {
            @Override
            public void onTrace(String msg) {
                Log.i(TAG, msg);
            }
        });

        this.btManager = (BluetoothManager) this.getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        this.btAdapter = this.btManager.getAdapter();

        if (this.btAdapter == null) {
            notifyOnTrace("BLE not supported");
            return;
        }

        if (!this.btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
        }

        this.startScan();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        this.currentTapAddress = intent.getStringExtra(INTENT_PARAM_INIT_ADDRESS);
        this.currentTapPassword = intent.getStringExtra(INTENT_PARAM_INIT_PASSWORD);

        return START_STICKY;
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
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
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
                notifyOnTrace("Found tap " + name + " " + address);

                availableTaps.put(address, result.getDevice());
                availableTapNames.put(address, name);

                Intent intent = new Intent(Constant.TAP_NOTIFICATION_SCAN);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);

                // auto-connect to this tap is it's last remembered
                if (address.equals(currentTapAddress)) {
                    notifyOnTrace("Auto-connecting to " + address);
                    connectToTap(address, currentTapPassword);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                notifyOnTrace("Scan failed with errorCode " + errorCode);
            }
        });

        this.isScanning = true;
        notifyOnTrace("Scan started...");
    }


    public void stopScan() {
        BluetoothLeScanner scanner = this.btAdapter.getBluetoothLeScanner();
        scanner.stopScan(new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                notifyOnTrace("Scan failed with errorCode" + errorCode);
            }
        });

        this.isScanning = false;
        notifyOnTrace("Scan stopped");
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

        // remember current device
        this.currentTapAddress = address;
        this.currentTapPassword = password;

        // NOTE: we want autoConnect parameter of connectGatt() to be true
        // because for some reason during recording tap is getting disconnected with status 19
        // so we would like it to be auto-connected once recording is done
        boolean autoConnect = true;
        this.btGatt = tap.connectGatt(this.getApplication(), autoConnect, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                // NOTE: from my experience I found the following on disconnection:
                // 1. When status == 19, then it means that a tap forces diconeection from its end
                //      (this can happen for example when the tap is in middle of recording an audio,
                //      or when password is incorrect)
                // 2. When status == 8, then it means that the tap is not in range (for example turned off)

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // If we are disconnected during authentication with status 19
                    // then it looks like password didn't match
                    if (isAuthenticating) {
                        notifyOnTrace("Password doesn't match");

                        isAuthenticating = false;

                        forgetCurrentTap();
                        gatt.disconnect();

                        Intent intent = new Intent(Constant.TAP_NOTIFICATION_TAP_AUTH_FAILED);
                        intent.putExtra(Constant.TAP_NOTIFICATION_TAP_AUTH_FAILED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    } else {
                        notifyOnTrace("Disconnected from to tap " + gatt.getDevice().getAddress() + " because of reason " + status);

                        Intent intent = new Intent(Constant.TAP_NOTIFICATION_TAP_DISCONNECTED);
                        intent.putExtra(Constant.TAP_NOTIFICATION_TAP_DISCONNECTED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    }
                } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    notifyOnTrace("Connected to tap " + gatt.getDevice().getAddress());

                    // this speeds things up?
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                    gatt.discoverServices();

                    // we are still not logically connected - once services are discovered
                    // we will send password for authentication
                } else {
                    notifyOnTrace("Connection failed with status " + status + " and newState " + newState);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    notifyOnTrace("Failed discovering services with status " + status);
                    return;
                }
                notifyOnTrace("Discovered services");

                // adjust our WRITE characteristic
                writeChrt = gatt
                        .getService(Constant.SERVICE_UUID)
                        .getCharacteristic(Constant.CHARACTERISTIC_UUID_TO_WRITE);
                writeChrt.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                // read characteristic
                readChrt = gatt
                        .getService(Constant.SERVICE_UUID)
                        .getCharacteristic(Constant.CHARACTERISTIC_UUID_TO_READ);
                enableChrtNotification(readChrt, true);

                // battery level characteristic
                batteryChrt = gatt
                        .getService(Constant.BATTERY_SERVICE_UUID)
                        .getCharacteristic(Constant.BATTERY_LEVEL_UUID);
                enableChrtNotification(batteryChrt, true);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] value = characteristic.getValue();

                if (characteristic == batteryChrt) {
                    notifyCharacteristicOnBatteryLevel(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                } else if (characteristic == readChrt) {
                    if (fileReceiver.isInProgress()) {
                        fileReceiver.append(value);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_CONNECTED)) {
                        // tap accepted password this is the last step when tap is considered authenticated
                        notifyOnTrace("Password is OK!");

                        isAuthenticating = false;

                        sendTime();

                        // notify about connected tap
                        Intent intent = new Intent(Constant.TAP_NOTIFICATION_TAP_AUTHED);
                        intent.putExtra(Constant.TAP_NOTIFICATION_TAP_AUTHED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_FILE_AUDIO)) {
                        notifyOnTrace("Start receiving audio file...");
                        fileReceiver.start(new FileReceiver.Callbacks() {
                            @Override
                            public void acknowledge(byte[] ack) {
                                // write directly avoiding writeQueue to speed things up in file transfer
                                writeChrt.setValue(ack);
                                btGatt.writeCharacteristic(writeChrt);
                            }

                            @Override
                            public void onComplete(Date time, byte[] buffer, int speed) {
                                notifyOnTrace("Received audio file recorded at " + time + " with size " + buffer.length + " bytes at speed " + speed);
                            }
                        });
                    } else if (Utils.startsWith(value, Constant.MESSAGE_FILE_LOG)) {
                        notifyOnTrace("Start receiving log file...");
                        fileReceiver.start(new FileReceiver.Callbacks() {
                            @Override
                            public void acknowledge(byte[] ack) {
                                // write directly avoiding writeQueue to speed things up in file transfer
                                writeChrt.setValue(ack);
                                btGatt.writeCharacteristic(writeChrt);
                            }

                            @Override
                            public void onComplete(Date time, byte[] buffer, int speed) {
                                notifyOnTrace("Received log file recorded at " + time + " with size " + buffer.length + " bytes at speed " + speed);
                            }
                        });
                    } else if (Utils.startsWith(value, Constant.MESSAGE_RECLN)) {
                        int number = Utils.extractInt(value, Constant.MESSAGE_RECLN);
                        notifyCharacteristicOnIntValue(Constant.DEVICE_PARAM_RECLN, number);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_PAUSELEVEL)) {
                        int number = Utils.extractInt(value, Constant.MESSAGE_PAUSELEVEL);
                        notifyCharacteristicOnIntValue(Constant.DEVICE_PARAM_PAUSELEVEL, number);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_PAUSELEN)) {
                        int number = Utils.extractInt(value, Constant.MESSAGE_PAUSELEN);
                        notifyCharacteristicOnIntValue(Constant.DEVICE_PARAM_PAUSELEN, number);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_ACCELERSENS)) {
                        int number = Utils.extractInt(value, Constant.MESSAGE_ACCELERSENS);
                        notifyCharacteristicOnIntValue(Constant.DEVICE_PARAM_ACCELERSENS, number);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_BTLOC)) {
                        int number = Utils.extractInt(value, Constant.MESSAGE_BTLOC);
                        notifyCharacteristicOnIntValue(Constant.DEVICE_PARAM_BTLOC, number);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_MIC)) {
                        int number = Utils.extractInt(value, Constant.MESSAGE_MIC);
                        notifyCharacteristicOnIntValue(Constant.DEVICE_PARAM_MIC, number);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_VERSION)) {
                        String string = Utils.extractString(value, Constant.MESSAGE_VERSION);
                        notifyCharacteristicOnStringValue(Constant.DEVICE_PARAM_VERSION, string);
                    } else if (Utils.startsWith(value, Constant.MESSAGE_UUID)) {
                        String string = Utils.extractString(value, Constant.MESSAGE_UUID);
                        notifyCharacteristicOnStringValue(Constant.DEVICE_PARAM_UUID, string);
                    }
                } else {
                    notifyOnTrace("onCharacteristicChanged unhandled value of " + characteristic.getUuid() + ": " + characteristic.getStringValue(0));
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (characteristic == batteryChrt) {
                    notifyCharacteristicOnBatteryLevel(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                } else {
                    notifyOnTrace("onCharacteristicRead unhandled read of " + characteristic.getUuid() + " with status " + status + ": " + Arrays.toString(characteristic.getValue()));
                }

                // file receiving has nothing to do with writeQueue, so skip processing it
                if (!fileReceiver.isInProgress()) {
                    // this is not file receiving - check maybe we have something more to write
                    chrtProcessingQueue.processNext();
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                // file receiving has nothing to do with writeQueue, so skip processing it
                if (!fileReceiver.isInProgress()) {
                    // this is not file receiving - check maybe we have something more to write
                    chrtProcessingQueue.processNext();
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                //super.onDescriptorRead(gatt, descriptor, status);
                //notifyOnTrace("onDescriptorRead " + descriptor.getUuid() + " with status " + status + ": " + descriptor.getValue());
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                //super.onDescriptorWrite(gatt, descriptor, status);
                //notifyOnTrace("onDescriptorWrite " + descriptor.getUuid() + " with status " + status + ": " + descriptor.getValue());

                // NOTE: for some reason we send password once descriptor for notification was written
                // send password to tap and wait until confirmation comes back (CONNECTED value in read chrt)
                sendPassword(password);
            }
        });

        this.chrtProcessingQueue = new ChrtProcessingQueue(this.btGatt);

        notifyOnTrace("Connecting to tap " + address + "...");
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

        this.isAuthenticating = true;

        this.chrtProcessingQueue.put(this.writeChrt, data3);

        notifyOnTrace("Sent password");
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

        this.chrtProcessingQueue.put(this.writeChrt, timeData);

        notifyOnTrace("Sent UTC time " + Arrays.toString(timeData));
    }


    public void disconnectFromCurrentTap() {
        this.forgetCurrentTap();
        this.btGatt.disconnect();
    }


    public void forgetCurrentTap() {
        this.currentTapAddress = null;
        this.currentTapPassword = null;

        notifyOnTrace("Forgotten current tap");
    }


    /**
     * Enables notification for a characteristic.
     * @param chrt
     * @param enable
     */
    private void enableChrtNotification(BluetoothGattCharacteristic chrt, boolean enable) {
        this.btGatt.setCharacteristicNotification(chrt, enable);

        BluetoothGattDescriptor descriptor = readChrt.getDescriptor(UUID.fromString(Constant.NOTIFICATION_DESCRIPTOR));
        if (descriptor != null) {
            byte[] val = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            this.btGatt.writeDescriptor(descriptor);
        }
    }




    public void readRECLN() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_RECLN);
    }

    public void readBTLOC() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_BTLOC);
    }

    public void readPAUSELEVEL() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_PAUSELEVEL);
    }

    public void readPAUSELEN() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_PAUSELEN);
    }

    public void readACCELERSENS() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_ACCELERSENS);
    }

    public void readMIC() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_MIC);
    }

    public void readVERSION() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_VERSION);
    }

    public void readUUID() {
        this.chrtProcessingQueue.put(this.writeChrt, Constant.MESSAGE_UUID);
    }

    public void readBatteryLevel() {
        this.chrtProcessingQueue.put(this.batteryChrt);
    }



    public void addCharacteristicValueListener(TapManager.CharacteristicValueListener listener) {
        this.characteristicValueListeners.add(listener);
    }

    public void removeCharacteristicValueListener(TapManager.CharacteristicValueListener listener){
        this.characteristicValueListeners.remove(listener);
    }

    private void notifyCharacteristicOnIntValue(final String param, final int value) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (TapManager.CharacteristicValueListener listener: characteristicValueListeners) {
                    listener.onIntValue(param, value);
                }
            }
        });
    }

    private void notifyCharacteristicOnStringValue(final String param, final String value) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (TapManager.CharacteristicValueListener listener: characteristicValueListeners) {
                    listener.onStringValue(param, value);
                }
            }
        });
    }

    private void notifyCharacteristicOnBatteryLevel(final int value) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (TapManager.CharacteristicValueListener listener : characteristicValueListeners) {
                    listener.onBatteryLevel(value);
                }
            }
        });
    }


    public void addTraceListener(TapManager.TraceListener listener) {
        this.traceListeners.add(listener);
    }

    public void removeTraceListener(TapManager.TraceListener listener){
        this.traceListeners.remove(listener);
    }

    private void notifyOnTrace(final String msg) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (TapManager.TraceListener listener : traceListeners) {
                    listener.onTrace(msg);
                }
            }
        });
    }

}
