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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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

    ScanCallback scanCallback;


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
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        this.currentTapAddress = intent.getStringExtra(INTENT_PARAM_INIT_ADDRESS);
        this.currentTapPassword = intent.getStringExtra(INTENT_PARAM_INIT_PASSWORD);

        // when there is information about last connected TAP and we are not connected to anything else
        // then start scan and try to auto-connect
        if (this.currentTapAddress != null && !this.isConnected()) {
            this.startScan(ScanSettings.SCAN_MODE_LOW_POWER);
        }

        return START_REDELIVER_INTENT;
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


    public void startScan(int scanMode) {
        BluetoothLeScanner scanner = null;
        if ((this.isScanning()) || ((scanner = this.btAdapter.getBluetoothLeScanner()) == null)) {
            return;
        }

        this.availableTaps.clear();
        this.availableTapNames.clear();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(scanMode)
                .build();
        List<ScanFilter> filters = new ArrayList<>();

        // scan for TAPs
        scanner.startScan(filters, settings, this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // filtering by service UUID above doesn't work
                // so we use trick from http://stackoverflow.com/a/24539704/444630
                // to extract service UUID
                List<UUID> uuids = Utils.parseUuidsByAdvertisedData(result.getScanRecord().getBytes());
                if (!uuids.contains(Constants.SERVICE_UUID)) {
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

                Intent intent = new Intent(Constants.TAP_NOTIFICATION_SCAN);
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

        notifyOnTrace("Scan started...");
    }


    public void stopScan() {
        BluetoothLeScanner scanner = null;
        if ((!this.isScanning()) || ((scanner = this.btAdapter.getBluetoothLeScanner()) == null)) {
            return;
        }

        scanner.stopScan(this.scanCallback);

        this.scanCallback = null;

        notifyOnTrace("Scan stopped");
    }


    public boolean isScanning() {
        return this.scanCallback != null;
    }


    public BluetoothDevice getConnectedTap() {
        return (this.btGatt != null) ? this.btGatt.getDevice() : null;
    }

    public boolean isConnected() {
        return this.btGatt != null;
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

        if (isConnected()) {
            this.btGatt.disconnect();
            this.btGatt.close();
            this.btGatt = null;
        }

        // NOTE: we want autoConnect parameter of connectGatt() to be false
        // because for some reason it i's much faster to connect to devices
        // we reconnect right after disconnection by oursefls
        boolean autoConnect = false;
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

                        disconnectFromCurrentTap();

                        Intent intent = new Intent(Constants.TAP_NOTIFICATION_TAP_AUTH_FAILED);
                        intent.putExtra(Constants.TAP_NOTIFICATION_TAP_AUTH_FAILED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    } else {
                        notifyOnTrace("Disconnected from tap " + gatt.getDevice().getAddress() + " because of reason " + status);

                        Intent intent = new Intent(Constants.TAP_NOTIFICATION_TAP_DISCONNECTED);
                        intent.putExtra(Constants.TAP_NOTIFICATION_TAP_DISCONNECTED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    }

                    // if we are not intentionally disconnected, try to connect to TAP next time it's available
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        // connect to the TAP once it's available again
                        connectToTap(address, password);
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
                        .getService(Constants.SERVICE_UUID)
                        .getCharacteristic(Constants.CHARACTERISTIC_UUID_TO_WRITE);
                writeChrt.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                // read characteristic
                readChrt = gatt
                        .getService(Constants.SERVICE_UUID)
                        .getCharacteristic(Constants.CHARACTERISTIC_UUID_TO_READ);
                enableChrtNotification(readChrt, true);

                // battery level characteristic
                batteryChrt = gatt
                        .getService(Constants.BATTERY_SERVICE_UUID)
                        .getCharacteristic(Constants.BATTERY_LEVEL_UUID);
                enableChrtNotification(batteryChrt, true);
            }

            @Override
            public void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] value = characteristic.getValue();

                if (characteristic == batteryChrt) {
                    notifyCharacteristicOnBatteryLevel(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                } else if (characteristic == readChrt) {
                    if (fileReceiver.isInProgress()) {
                        fileReceiver.append(value);
                    } else if (Utils.startsWith(value, Constants.MESSAGE_CONNECTED)) {
                        // tap accepted password this is the last step when tap is considered authenticated
                        notifyOnTrace("Password is OK!");

                        isAuthenticating = false;

                        sendTime();

                        // notify about connected tap
                        Intent intent = new Intent(Constants.TAP_NOTIFICATION_TAP_AUTHED);
                        intent.putExtra(Constants.TAP_NOTIFICATION_TAP_AUTHED_PARAM, gatt.getDevice().getAddress());
                        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                    } else if (Utils.startsWith(value, Constants.MESSAGE_FILE_AUDIO)) {
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
                                notifyOnTrace("Received audio file recorded at " + time + " with size " + buffer.length + " bytes at speed " + speed + " B/s");

                                SimpleDateFormat formatter = new SimpleDateFormat(Constants.AudioFileNameFormat);
                                String fileName = formatter.format(time);
                                String absolutePath = Utils.writeFile(MyleBleService.this, "audio", fileName, buffer);

                                notifyOnTrace("The file is written to " + absolutePath);

                                // notify about connected tap
                                Intent intent = new Intent(Constants.TAP_NOTIFICATION_FILE);
                                intent.putExtra(Constants.TAP_NOTIFICATION_FILE_PATH_PARAM, absolutePath);
                                intent.putExtra(Constants.TAP_NOTIFICATION_FILE_TIME_PARAM, time.getTime());
                                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent);
                            }
                        });
                    } else if (Utils.startsWith(value, Constants.MESSAGE_FILE_LOG)) {
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
                                notifyOnTrace("Received log file recorded at " + time + " with size " + buffer.length + " bytes at speed " + speed + " B/s");

                                SimpleDateFormat formatter = new SimpleDateFormat(Constants.LogFileNameFormat);
                                String fileName = formatter.format(time);
                                String absolutePath = Utils.writeFile(MyleBleService.this, "logs", fileName, buffer);

                                notifyOnTrace("The file is written to " + absolutePath);
                            }
                        });
                    } else if (Utils.startsWith(value, Constants.READ_RECLN)) {
                        int number = Utils.extractInt(value, Constants.READ_RECLN);
                        notifyCharacteristicOnIntValue(Constants.DEVICE_PARAM_RECLN, number);
                    } else if (Utils.startsWith(value, Constants.READ_PAUSELEVEL)) {
                        int number = Utils.extractInt(value, Constants.READ_PAUSELEVEL);
                        notifyCharacteristicOnIntValue(Constants.DEVICE_PARAM_PAUSELEVEL, number);
                    } else if (Utils.startsWith(value, Constants.READ_PAUSELEN)) {
                        int number = Utils.extractInt(value, Constants.READ_PAUSELEN);
                        notifyCharacteristicOnIntValue(Constants.DEVICE_PARAM_PAUSELEN, number);
                    } else if (Utils.startsWith(value, Constants.READ_ACCELERSENS)) {
                        int number = Utils.extractInt(value, Constants.READ_ACCELERSENS);
                        notifyCharacteristicOnIntValue(Constants.DEVICE_PARAM_ACCELERSENS, number);
                    } else if (Utils.startsWith(value, Constants.READ_BTLOC)) {
                        int number = Utils.extractInt(value, Constants.READ_BTLOC);
                        notifyCharacteristicOnIntValue(Constants.DEVICE_PARAM_BTLOC, number);
                    } else if (Utils.startsWith(value, Constants.READ_MIC)) {
                        int number = Utils.extractInt(value, Constants.READ_MIC);
                        notifyCharacteristicOnIntValue(Constants.DEVICE_PARAM_MIC, number);
                    } else if (Utils.startsWith(value, Constants.READ_VERSION)) {
                        String string = Utils.extractString(value, Constants.READ_VERSION);
                        notifyCharacteristicOnStringValue(Constants.DEVICE_PARAM_VERSION, string);
                    } else if (Utils.startsWith(value, Constants.READ_UUID)) {
                        String string = Utils.extractString(value, Constants.READ_UUID);
                        notifyCharacteristicOnStringValue(Constants.DEVICE_PARAM_UUID, string);
                    }
                } else {
                    notifyOnTrace("onCharacteristicChanged unhandled value of " + characteristic.getUuid() + ": " + Arrays.toString(characteristic.getValue()));
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
        this.btGatt.close();
        this.btGatt = null;
    }


    public void forgetCurrentTap() {
        this.currentTapAddress = null;
        this.currentTapPassword = null;

        notifyOnTrace("Forgotten current tap");
    }


    /**
     * Enables notification for a characteristic.
     *
     * @param chrt
     * @param enable
     */
    private void enableChrtNotification(BluetoothGattCharacteristic chrt, boolean enable) {
        this.btGatt.setCharacteristicNotification(chrt, enable);

        BluetoothGattDescriptor descriptor = readChrt.getDescriptor(UUID.fromString(Constants.NOTIFICATION_DESCRIPTOR));
        if (descriptor != null) {
            byte[] val = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            this.btGatt.writeDescriptor(descriptor);
        }
    }


    public void readRECLN() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_RECLN);
    }

    public void readBTLOC() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_BTLOC);
    }

    public void readPAUSELEVEL() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_PAUSELEVEL);
    }

    public void readPAUSELEN() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_PAUSELEN);
    }

    public void readACCELERSENS() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_ACCELERSENS);
    }

    public void readMIC() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_MIC);
    }

    public void readVERSION() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_VERSION);
    }

    public void readUUID() {
        this.chrtProcessingQueue.put(this.writeChrt, Constants.READ_UUID);
    }

    public void readBatteryLevel() {
        this.chrtProcessingQueue.put(this.batteryChrt);
    }


    public void writeRECLN(int value) {
        String temp = String.format(Locale.getDefault(), "%02d", value);
        String str = Constants.WRITE_RECLN + temp;

        this.chrtProcessingQueue.put(this.writeChrt, str.getBytes());
    }

    public void writePAUSELEVEL(int value) {
        String temp = String.format(Locale.getDefault(), "%03d", value);
        String str = Constants.WRITE_PAUSELEVEL + temp;

        this.chrtProcessingQueue.put(this.writeChrt, str.getBytes());
    }

    public void writePAUSELEN(int value) {
        String temp = String.format(Locale.getDefault(), "%02d", value);
        String str = Constants.WRITE_PAUSELEN + temp;

        this.chrtProcessingQueue.put(this.writeChrt, str.getBytes());
    }

    public void writeACCELERSENS(int value) {
        String temp = String.format(Locale.getDefault(), "%03d", value);
        String str = Constants.WRITE_ACCELERSENS + temp;

        this.chrtProcessingQueue.put(this.writeChrt, str.getBytes());
    }

    public void writeMIC(int value) {
        String temp = String.format(Locale.getDefault(), "%03d", value);
        String str = Constants.WRITE_MIC + temp;

        this.chrtProcessingQueue.put(this.writeChrt, str.getBytes());
    }

    public void writeBTLOC(int value) {
        String str = Constants.WRITE_BTLOC + value;

        this.chrtProcessingQueue.put(this.writeChrt, str.getBytes());
    }

    public void writePASSWORD(String value) {
        byte[] a = (Constants.WRITE_PASSWORD + String.format(Locale.getDefault(), "%c", (byte) value.length())).getBytes();
        byte[] b = value.getBytes();

        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        this.chrtProcessingQueue.put(this.writeChrt, c);
    }


    public void addCharacteristicValueListener(TapManager.CharacteristicValueListener listener) {
        this.characteristicValueListeners.add(listener);
    }

    public void removeCharacteristicValueListener(TapManager.CharacteristicValueListener listener) {
        this.characteristicValueListeners.remove(listener);
    }

    private void notifyCharacteristicOnIntValue(final String param, final int value) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (TapManager.CharacteristicValueListener listener : characteristicValueListeners) {
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
                for (TapManager.CharacteristicValueListener listener : characteristicValueListeners) {
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

    public void removeTraceListener(TapManager.TraceListener listener) {
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
