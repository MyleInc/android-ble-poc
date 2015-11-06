package com.getmyle.mylesdk;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.getmyle.R;
import com.getmyle.mylesdk.BleWrapper.BLEWrapperListener;
import com.getmyle.mylesdk.Constant.ConnectState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Handle bluetooth tasks.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class MyleService1 extends Service {

    private static final String TAG = MyleService1.class.getSimpleName();

    private static int FOREGROUND_ID = 1338;
    private static final int AUDIO_HEADER_SIZE = 12;
    private static final int CREDITBLE = 282;//423;
    private static int numBytesTransfered = 0;
    private static int fileLength = 0;
    private String mPassword;
    private BluetoothGattCharacteristic mWriteCharacter;
    private BluetoothGattCharacteristic mRecvCharacter;
    private BluetoothGattCharacteristic mBatteryLevel;
    private BleWrapper mBleWrapper;
    private List<MyleServiceListener> mListener = new ArrayList<MyleServiceListener>();
    private List<ParameterListener> mParameterListener = new ArrayList<ParameterListener>();
    private boolean isReceivingAudioFile;
    private boolean isReceivingLogFile;
    private int audioRecvLength;
    private int logRecvLength;
    private int recvCountAudio;
    private int recvCountLog;
    private byte[] audioBuffer;
    private byte[] logBuffer;
    private int second, min, hour, date, month, year;
    private long startRecvTime, stopRecvTime;
    private Map<String, BluetoothDevice> mListDevice = new HashMap<String, BluetoothDevice>();
    private final IBinder mBinder = new LocalBinder();
    private static boolean sIsRunning;
    private boolean isConnecting;
    private int mReceiveMode;
    private Handler mUiThreadHandler = new Handler();


    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate() {
        super.onCreate();

        if (sIsRunning) {
            Log.v(TAG, "Already run");
            return;
        }

        Log.i(TAG, "onCreate");
        sIsRunning = true;

        // Create foreground notification.
        // Service is never killed by system when low memory.
        //startForeground(FOREGROUND_ID, buildForegroundNotification("Myle"));

        // Init bluetooth wrapper
        //initBlewrapper();

        mBluetoothManager = (BluetoothManager) this.getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        LocalBroadcastManager.getInstance(this.getApplication()).registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Log.i(TAG, "Scan started");



        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
//        ScanFilter filter = new ScanFilter.Builder()
//                .setServiceUuid(ParcelUuid.fromString(Constant.SERVICE_UUID))
//                .setServiceUuid(ParcelUuid.fromString(Constant.BATTERY_SERVICE_UUID))
//                .build();
//        filters.add(filter);

        final Service me = this;

        // scan for TAPs
        scanner.startScan(filters, settings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i(TAG, "found device " + callbackType + " " + result.getDevice().getAddress());
                if (mAvailableTaps.containsKey(result.getDevice().getAddress())) { return; }

                mAvailableTaps.put(result.getDevice().getAddress(), result.getDevice());
                mAvailableTapNames.put(result.getDevice().getAddress(), getNameOfScanDevice(result.getScanRecord().getBytes()));

                Intent intent = new Intent(Constant.TAP_NOTIFICATION_SCAN);
                LocalBroadcastManager.getInstance(me.getApplication()).sendBroadcast(intent);
            }
        });

        return START_STICKY;
    }


    HashMap<String, BluetoothDevice> mAvailableTaps = new HashMap<>(10);
    HashMap<String, String> mAvailableTapNames = new HashMap<>(10);


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG,"Action received: "+action);
            if(action.equals(BluetoothDevice.ACTION_UUID)) {
                BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG,"Received uuids for "+btd.getName());
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                StringBuilder sb = new StringBuilder();
                List<String> uuids = new ArrayList<String>(uuidExtra.length);
                if(uuidExtra != null) {
                    for (int i = 0; i < uuidExtra.length; i++) {
                        sb.append(uuidExtra[i].toString()).append(',');
                        uuids.add(uuidExtra[i].toString());
                    }
                }
                Log.i(TAG, "ACTION_UUID received for " + btd.getName() + " uuids: " + sb.toString());
                //ListContent.addItemWithUUIDs(btd, uuids);
            }
        }
    };












    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        stopForeground(true);
        mBleWrapper.stopScanning();
        mBleWrapper.diconnect();
        mBleWrapper.close();
        mBleWrapper.finalize();
        sIsRunning = false;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    public class LocalBinder extends Binder {
        public MyleService1 getServerInstance() {
            return MyleService1.this;
        }
    }

    public void addMyleServiceListener(MyleServiceListener listener) {
        mListener.add(listener);
    }

    public void removeMyleServiceListener(MyleServiceListener listener) {
        mListener.remove(listener);
    }

    public void addParameterListener(ParameterListener listener) {
        mParameterListener.add(listener);
    }

    public void removeParameterListener(ParameterListener listener) {
        mParameterListener.add(listener);
    }

    public int getReceiveByteAudio() {
        return recvCountAudio;
    }

    public static boolean isRunning() {
        return sIsRunning;
    }

    public boolean isConnected() {
        return mBleWrapper.isConnected();
    }

    public void forgetCurrentDevice() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .remove(Constant.SharedPrefencesKeyword.LAST_CONNECTED_TAP_UUID)
                .apply();
    }

    public void startScan() {
        notifyListeners("Start scanning");

        Log.i(TAG, "Start scanning");
        mBleWrapper.startScanning();
    }

    public void stopScan() {
        mListDevice.clear();
        mBleWrapper.stopScanning();
    }

    public void connect(final String address, String pass) {
        /* Save connecting address */
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constant.SharedPrefencesKeyword.LAST_CONNECTED_TAP_UUID, address)
                .apply();
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constant.SharedPrefencesKeyword.LAST_CONNECTED_TAP_PASS, pass)
                .apply();

        mPassword = pass;

        stopScan();

		/* Gatt connect() function must be called in UI thread */
        if (Looper.myLooper() == null) Looper.prepare();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {

            @Override
            public void run() {
                mBleWrapper.connect(address);
            }
        });

        notifyListeners("Connecting");
    }

    public void disconnect() {
        mBleWrapper.diconnect();
        clearFlags();
        stopSelf();
    }

    public void send(byte[] data) {
        mBleWrapper.writeDataToCharacteristic(mWriteCharacter, data);
    }

    private void initBlewrapper() {
        Log.i(TAG, "initBlewrapper");
        mBleWrapper = new BleWrapper(getApplicationContext());
        mBleWrapper.setBLEWrapperListener(bleWrapperListener);
    }

    public void requestBatteryLevelValue(){
        mBleWrapper.requestCharacteristicValue(mBatteryLevel);
    }

    public void enableBatteryLevelNotification(){
        mBleWrapper.setNotificationForCharacteristic(mBatteryLevel, true);
    }

    /**
     * Sync device time with phone time
     */
    private void syncTime() {
        Calendar c = Calendar.getInstance();

        int second = c.get(Calendar.SECOND);
        int minute = c.get(Calendar.MINUTE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int date = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        int year = (c.get(Calendar.YEAR) - 2000) & 0xff;

        byte[] syncTimeData = new byte[]{'5', '5', '0', '0',
                (byte) second, (byte) minute,
                (byte) hour, (byte) date,
                (byte) month, (byte) year};

        for (int i = 0; i < syncTimeData.length; i++) {
            Log.i(TAG, "" + syncTimeData[i]);
        }

        send(syncTimeData);

        notifyListeners("sync time done");
    }


    /**
     * Build notification for make foregroundService.
     * then service will not be killed when system low memory.
     *
     * @param filename
     * @return
     */
    private Notification buildForegroundNotification(String filename) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);

        b.setOngoing(true)
                .setContentTitle("Myle")
                .setContentText(filename)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker("Myle");

        return (b.build());
    }

    /**
     * Send password to device
     */
    private void sendPassword() {
        byte data1[] = new byte[]{'5', '5', '0', '1', (byte) mPassword.length()};
        byte data2[] = mPassword.getBytes();
        byte data3[] = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);

        send(data3);

        if (mListener != null) {
            Log.i(TAG, "Sent password");
            notifyListeners("sent password");
        }
    }

    /**
     * FIXME
     * Get name of scan device for advertisement.
     * Android core can't read name.
     * You can use another app on Google Play to verify
     *
     * @param scanRecord
     * @return device name
     */
    private String getNameOfScanDevice(byte[] scanRecord) {
        int nameLength = 0;
        int i = 23;	 /* Start value of name*/

        // Get name's length
        do {
            nameLength++;
            i++;
        } while (scanRecord[i] != 0);

        // Get name
        byte[] nameArr = new byte[nameLength];
        int k = 0;
        for (i = 23; i < 23 + nameLength; i++) {
            nameArr[k] = scanRecord[i];
            k++;
        }

        // Convert to string
        String name = "";
        try {
            name = new String(nameArr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return name;
    }

    /* Save file into sdcard */
    private void save2SD(byte[] sData, String name) {
        String sRoot = null;
        String sPath = null;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            sRoot = Environment.getExternalStorageDirectory().toString();
        else
            return;

        sPath = sRoot.concat("/").concat(this.getString(R.string.app_name));
        if (covertByte2File(sPath, name, sData)) {
            String sMsg = ("save to:").concat(sPath).concat("/").concat(name);
            notifyListeners(sMsg);
        } else {
            notifyListeners("Save fail");
        }
    }

    /* Copy binary data into a file */
    private boolean covertByte2File(String sPath, String sFile, byte[] bData) {
        try {
            File fhd = new File(sPath);
            if (!fhd.exists())
                if (!fhd.mkdirs())
                    return false;

            fhd = new File(sPath + "/" + sFile);
            if (fhd.exists())
                fhd.delete();

            FileOutputStream fso = new FileOutputStream(fhd);
            fso.write(bData);
            fso.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void getAudioFileTimestamp(byte[] data) {
        // Get date time. byte 8 to 11.
        int temp = (int) (((data[8]) & 0xff) + ((data[9]) & 0xff) * (Math.pow(16, 2)) +
                ((data[10]) & 0xff) * (Math.pow(16, 4)) + ((data[11]) & 0xff) * (Math.pow(16, 6)));

        second = ((temp & 0x1f) * 2);
        temp = temp >> 5;

        min = temp & 0x3f;
        temp = temp >> 6;

        hour = temp & 0x1f;
        temp = temp >> 5;

        date = temp & 0x1f;
        temp = temp >> 5;

        month = temp & 0xf;
        temp = temp >> 4;

        year = (temp & 0x7f) + 1980;

        notifyListeners(year + "-" + month + "-" + date + "_" + hour + ":" + min + ":" + second);
    }

    private void getAudioFileSize(byte[] data) {
        // Get audio length. Byte 4 to byte 7.
        audioRecvLength = (int) ((data[4] & 0xff) + (data[5] & 0xff) * (Math.pow(16, 2)) +
                (data[6] & 0xff) * (Math.pow(16, 4)) + (data[7] & 0xff) * (Math.pow(16, 6)));
        notifyListeners("audio file size = " + audioRecvLength + " bytes");

        // Initialize receive buffer
        audioBuffer = new byte[audioRecvLength];
    }

    private void getLogFileSize(byte[] data) {
        // Get audio length. Byte 4 to byte 7.
        logRecvLength = (int) ((data[4] & 0xff) + (data[5] & 0xff) * (Math.pow(16, 2)) +
                (data[6] & 0xff) * (Math.pow(16, 4)) + (data[7] & 0xff) * (Math.pow(16, 6)));
        notifyListeners("log file size = " + logRecvLength + " bytes");

        // Initialize receive buffer
        logBuffer = new byte[logRecvLength];
    }

    private void readAudioHeader(byte[] header) {
        // DEBUG
        for (int i = 0; i < AUDIO_HEADER_SIZE; i++) {
            Log.i(TAG, "" + header[i]);
        }

        getAudioFileSize(header);
        getAudioFileTimestamp(header);
    }

    private void readLogHeader(byte[] header) {
        // DEBUG
        for (int i = 0; i < AUDIO_HEADER_SIZE; i++) {
            Log.i(TAG, "" + header[i]);
        }

        getLogFileSize(header);
        getAudioFileTimestamp(header);
    }

    private void receiveAudioFile(byte[] data) {
        int numBytes = CREDITBLE;

        if (audioRecvLength == 0) {
            readAudioHeader(data);

            if (audioRecvLength == 0) return;

            // Get start receive audio file  time
            startRecvTime = System.currentTimeMillis();

            isReceivingAudioFile = true;
            notifyListeners("Receiving audio data...");

            numBytes = CREDITBLE;
            numBytesTransfered = 0;
            byte[] ack = new byte[]{(byte) (numBytes & 0xff), (byte)(numBytes >> 8)};
            send(ack);

        } else if (recvCountAudio < audioRecvLength) {
            // Get audio data
            System.arraycopy(data, 0, audioBuffer, recvCountAudio, data.length);
            recvCountAudio += data.length;
            Log.i(TAG, "recvCountAudio = " + recvCountAudio);

			/* End of audio file */
            if (recvCountAudio >= audioRecvLength) {
                Log.i(TAG, "Receive audio done");
                // Save audio file to sdcard
                save2SD(audioBuffer, year + "-" + month + "-" + date + "_" + hour + ":" + min + ":" + second + ".wav");

                // Calc transfer audio file time
                stopRecvTime = System.currentTimeMillis();
                long timeTransfer = (stopRecvTime - startRecvTime);// to second

                notifyListeners("Elapse " + timeTransfer / 1000 + " seconds");
                // mListener.log("Speed  = " + (recvCountAudio/timeTransfer) + " B/s");
                notifyListeners("Transfer Speed " + (int)(audioRecvLength/(timeTransfer/1000)) + " B/s");
                notifyListeners("Receive done");

                // Clear variable, flags
                audioRecvLength = 0;
                recvCountAudio = 0;
                fileLength = 0;
                isReceivingAudioFile = false;
                mReceiveMode = Constant.RECEIVE_MODE.RECEIVE_NONE;
            }
            else {
                numBytesTransfered += data.length;
                fileLength += numBytesTransfered;

                numBytes = CREDITBLE;
                if(audioRecvLength - recvCountAudio < CREDITBLE)
                {
                    numBytes = audioRecvLength - recvCountAudio;
                }

                if(numBytesTransfered >= numBytes){
                    numBytesTransfered = 0;
                    // Send ack is number bytes received.
                    byte[] ack = new byte[]{(byte) (numBytes & 0xff),  (byte)(numBytes >> 8)};
                    send(ack);
                }
            }
        }
    }

    private void receiveLogFile(byte[] data) {
        int numBytes = CREDITBLE;

        if (logRecvLength == 0) {
            readLogHeader(data);

            // Get start receive audio file  time
            startRecvTime = System.currentTimeMillis();

            isReceivingLogFile = true;
            notifyListeners("Receiving log data...");

            numBytesTransfered = 0;
            numBytes = CREDITBLE;
            byte[] ack = new byte[]{(byte) (numBytes & 0xff),  (byte)(numBytes >> 8)};
            send(ack);

        } else if (recvCountLog < logRecvLength) {
            // Get audio data
            System.arraycopy(data, 0, logBuffer, recvCountLog, data.length);
            recvCountLog += data.length;
            Log.i(TAG, "recvCountLog = " + recvCountLog);

			/* End of log file */
            if (recvCountLog >= logRecvLength) {
                Log.i(TAG, "Receive log done");

                // Save log file to sdcard
                save2SD(logBuffer, year + "-" + month + "-" + date + "_" + hour + ":" + min + ":" + second + "_log.txt");

                // Calc transfer audio file time
                stopRecvTime = System.currentTimeMillis();
                long timeTransfer = (stopRecvTime - startRecvTime);// to second

                notifyListeners("Elapse " + timeTransfer / 1000 + " seconds");
                notifyListeners("Speed  = " + (recvCountLog / (timeTransfer / 1000)) + " B/s");
                notifyListeners("Receive log done");

                // Clear variable, flags
                logRecvLength = 0;
                recvCountLog = 0;
                isReceivingLogFile = false;
                mReceiveMode = Constant.RECEIVE_MODE.RECEIVE_NONE;
            }
            else {
                numBytesTransfered += data.length;
                fileLength += numBytesTransfered;

                numBytes = CREDITBLE;
                if(logRecvLength - recvCountLog < CREDITBLE)
                {
                    numBytes = logRecvLength - recvCountLog;
                }

                if(numBytesTransfered >= numBytes){
                    numBytesTransfered = 0;
                    // Send ack is number bytes received.
                    byte[] ack = new byte[]{(byte) (numBytes & 0xff),  (byte)(numBytes >> 8)};
                    send(ack);
                }
            }
        }
    }

    /**
     * Analyze received data
     *
     * @param str
     * @return true: data is not audio
     * false: data is audio
     */
    private boolean readParameter(String str) {
        try {
            if (str.contains(Constant.DEVICE_PARAM_RECLN)) {
                String result = str.replace(Constant.DEVICE_PARAM_RECLN, "");
                int x = Integer.parseInt(result);
                notifyListeners(Constant.DEVICE_PARAM_RECLN, x, result);
                return true;
            } else if (str.contains(Constant.DEVICE_PARAM_PAUSELEVEL)) {
                String result = str.replace(Constant.DEVICE_PARAM_PAUSELEVEL, "");
                int x = Integer.parseInt(result);
                notifyListeners(Constant.DEVICE_PARAM_PAUSELEVEL, x, result);
                return true;
            } else if (str.contains(Constant.DEVICE_PARAM_PAUSELEN)) {
                String result = str.replace(Constant.DEVICE_PARAM_PAUSELEN, "");
                int x = Integer.parseInt(result);
                notifyListeners(Constant.DEVICE_PARAM_PAUSELEN, x, result);
                return true;
            } else if (str.contains(Constant.DEVICE_PARAM_ACCELERSENS)) {
                String result = str.replace(Constant.DEVICE_PARAM_ACCELERSENS, "");
                int x = Integer.parseInt(result);
                notifyListeners(Constant.DEVICE_PARAM_ACCELERSENS, x, result);
                return true;
            } else if (str.contains(Constant.DEVICE_PARAM_BTLOC)) {
                String result = str.replace(Constant.DEVICE_PARAM_BTLOC, "");
                int x = Integer.parseInt(result);
                notifyListeners(Constant.DEVICE_PARAM_BTLOC, x, result);
                return true;
            } else if (str.contains(Constant.DEVICE_PARAM_MIC)) {
                String result = str.replace(Constant.DEVICE_PARAM_MIC, "");
                int x = Integer.parseInt(result);
                notifyListeners(Constant.DEVICE_PARAM_MIC, x, result);
                return true;
            } else if (str.contains(Constant.DEVICE_PARAM_VERSION)) {
                String result = str.replace(Constant.DEVICE_PARAM_VERSION, "");
                notifyListeners(Constant.DEVICE_PARAM_VERSION, 0, result);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean checkPassword(String password) {
        if (password.equals("CONNECTED")) {
            notifyListeners("connected");
            syncTime();

            return true;
        }

		/* Not password value */
        return false;
    }


    /**
     * Check receive data if we know it's not audio
     *
     * @return true: data is connection information or parameter
     * false: data is audio header
     */
    private boolean analyzeNotAudioFile(byte[] data) {
        String string = null;

        try {
            // Convert byte[] to String
            string = new String(data, "UTF-8");
            Log.i(TAG, "receive = " + string);

            // Check parameter
            if (string.contains("5503")) {
                String[] temp = string.split("5503");
                if (temp.length > 1) {
                    if (readParameter(temp[1])) return true;
                }
            } else if (string.contains("5504")) {
                String[] temp = string.split("5504");

                if (temp.length > 1) {
                    if (temp[1].equals("0")) {
                        mReceiveMode = Constant.RECEIVE_MODE.RECEIVE_AUDIO_FILE;
                    } else if (temp[1].equals("1")) {
                        mReceiveMode = Constant.RECEIVE_MODE.RECEIVE_LOG_FILE;
                    }
                }
            }

            // Check password
            if (checkPassword(string)) return true;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // this is needed to bind TapManager to this service,
        // so TapManager has access to current instance
        return mBinder;
    }

    /**************
     * BLEWrapper listener
     **************/
    BLEWrapperListener bleWrapperListener = new BLEWrapperListener() {

        @Override
        public void onReceiveData(BluetoothGatt gatt,
                                  BluetoothGattCharacteristic charac, byte[] data) {

            if (!isReceivingAudioFile && !isReceivingLogFile) {
                if (analyzeNotAudioFile(data)) return;
            }

            if (mReceiveMode == Constant.RECEIVE_MODE.RECEIVE_AUDIO_FILE) {
                receiveAudioFile(data);
            } else if (mReceiveMode == Constant.RECEIVE_MODE.RECEIVE_LOG_FILE) {
                receiveLogFile(data);
            }
        }

        @Override
        public void setNotificationForCharacteristicBattery(BluetoothGattCharacteristic ch, boolean enabled) {
            if (ch != null) {
                mBleWrapper.setNotificationForCharacteristic(ch, true);
            }
        }

        @Override
        public void onReceiveBatteryLevel(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic charac, byte[] data) {

            notifyListeners("onReceiveBatteryLevel = " + data[0]);
        }

        @Override
        public void onReadBatteryLevel(BluetoothGatt gatt,
                                       BluetoothGattCharacteristic charac, byte[] data) {

            notifyListeners("onReadBatteryLevel = " + data[0]);
        }

        @Override
        public void onListCharacteristics(List<BluetoothGattCharacteristic> chars) {
            for (BluetoothGattCharacteristic charac : chars) {
                Log.i(TAG, "characterestic = " + charac.getUuid());
                if (charac.getUuid().equals(Constant.CHARACTERISTIC_UUID_TO_WRITE)) {
                    mWriteCharacter = charac;
                    mWriteCharacter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                } else if (charac.getUuid().equals(Constant.CHARACTERISTIC_UUID_TO_READ)) {
                    mRecvCharacter = charac;
                    mBleWrapper.setNotificationForCharacteristic(mRecvCharacter, true);
                }else if(charac.getUuid().equals(Constant.BATTERY_LEVEL_UUID)){
                    mBatteryLevel = charac;
                   // mBleWrapper.setNotificationForCharacteristic(mBatteryLevel, true);
                }
            }
            notifyListeners("list charecterestic done");
        }

        @Override
        public void onFoundService(List<BluetoothGattService> services) {
            for (BluetoothGattService service : services) {
                Log.i(TAG, "Service found = " + service.getUuid());
                if (service.getUuid().equals(Constant.SERVICE_UUID)) {
                    mBleWrapper.getCharacteristicsForService(service);
                }else if (service.getUuid().equals(Constant.BATTERY_SERVICE_UUID)) {
                    mBleWrapper.getCharacteristicsForService(service);
                }
            }
            notifyListeners("list service done");
        }

        @Override
        public void onFoundDevice(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (isConnecting) return;

            // Already found out before
            if (mListDevice.get(device.getAddress()) != null) { return; }

            /* Found new device */
            mListDevice.put(device.getAddress(), device);
            notifyListeners(device, getNameOfScanDevice(scanRecord));

            // now try to auto-connect to last remembered device
            String uuid = PreferenceManager.getDefaultSharedPreferences(MyleService1.this)
                    .getString(Constant.SharedPrefencesKeyword.LAST_CONNECTED_TAP_UUID, null);
            String pass = PreferenceManager.getDefaultSharedPreferences(MyleService1.this)
                    .getString(Constant.SharedPrefencesKeyword.LAST_CONNECTED_TAP_PASS, null);

            if ((uuid != null && pass != null) && (device.getAddress().equalsIgnoreCase(uuid))) {  /* Device that connected before */
                Log.i(TAG, "Connecting ...");
                isConnecting = true;
                connect(device.getAddress(), pass);
            }
        }

        @Override
        public void onDisconnected() {
            Log.i(TAG, "Disconnect");
            clearFlags();

            // Re-connect
            mBleWrapper.close();
            initBlewrapper();
            startScan();
        }

        @Override
        public void onConnectResult(ConnectState resultCode, String error) {
            isConnecting = false;

            if (resultCode == Constant.ConnectState.BLE_CONNECT_FAIL) {
                notifyListeners("Gatt error = " + error);

                clearFlags();

                // Re-connect
                mBleWrapper.stopScanning();
                mBleWrapper.diconnect();

                initBlewrapper();
                startScan();
            } else {
                notifyListeners("Connected! Starting services discovery..");

                // In our case we would also like automatically to call for services discovery
                mBleWrapper.startServicesDiscovery();
            }
        }

        @Override
        public void onDidConfigureGatt() {
            sendPassword();
        }
    };

    public void notifyListeners(final String log) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (MyleServiceListener listener : mListener) {
                    listener.log(log);
                }
            }
        });
    }

    public void notifyListeners(final BluetoothDevice device, final String deviceName) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (MyleServiceListener listener : mListener) {
                    listener.onFoundNewDevice(device, deviceName);
                }
            }
        });
    }

    public void notifyListeners(final String name, final int intValue, final String strValue) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (ParameterListener listener : mParameterListener) {
                    listener.onReceive(name, intValue, strValue);
                }
            }
        });
    }

    private void clearFlags() {
        recvCountAudio = 0;
        recvCountLog = 0;
        audioRecvLength = 0;
        logRecvLength = 0;
        isReceivingAudioFile = false;
        isReceivingLogFile = false;
        isConnecting = false;
    }

    public interface MyleServiceListener {
        void onFoundNewDevice(BluetoothDevice device, String deviceName);

        void log(String log);
    }

    public interface ParameterListener {
        void onReceive(String name, int intValue, String strValue);
    }
}
