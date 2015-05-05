package com.example.myle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.myle.BleWrapper.BLEWrapperListener;
import com.example.myle.Constant.ConnectState;

/*
 * Handle bluetooth tasks.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class MyleService extends Service {
	private static final String TAG = "MyleService";
	private static int FOREGROUND_ID = 1338;
	private static final int AUDIO_HEADER_SIZE = 12;
	private String mPassword;
	private BluetoothGattCharacteristic mWriteCharacter;
	private BluetoothGattCharacteristic mRecvCharacter;
	private BleWrapper mBleWrapper;
	private MyleServiceListener mListener;
	private ParameterListener mParameterListener;
	private boolean isReceivingAudioFile;
	private int dataRecvLength = 0;
	private int recvCountAudio = 0;
	private byte[] dataRecv;
	private int second, min, hour, date, month, year;
	private long startRecvTime, stopRecvTime;
	private ArrayList<BluetoothDevice> mListDevice = new ArrayList<BluetoothDevice>();
	private final IBinder mBinder = new LocalBinder();
	private static boolean sIsRunning;

	@Override
	public void onCreate() {
		super.onCreate();

		if (sIsRunning) {
			Log.v(TAG, "Already run");
			return;
		}
		
		sIsRunning = true;
		
		// Create foreground notification. 
		// Service is never killed by system when low memory.
		startForeground(FOREGROUND_ID, buildForegroundNotification("Myle"));
		
		// Init bluetooth wrapper
		initBlewrapper();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startID) {
	    return START_NOT_STICKY; 
	}
	
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
	}
	
	public class LocalBinder extends Binder {
		  public MyleService getServerInstance() {
			  return MyleService.this;
		  }
	}
	
	public void setMyleServiceListener(MyleServiceListener listener) {
		this.mListener = listener;
	}
	
	public void setParameterListener(ParameterListener listener) {
		this.mParameterListener = listener;
	}
	
	public static boolean isRunning() {
		return sIsRunning;
	}
	
	public boolean isConnected() {
		return mBleWrapper.isConnected();
	}
	
	public void setPassword(String password) {
		mPassword = password;
	}
	
	public void startScan() {
        if (mListener != null) {
            mListener.log("Start scanning");
        }

		mBleWrapper.startScanning();
	}
	
	public void stopScan() {
		mListDevice.clear();
		mBleWrapper.stopScanning();
	}
	
	public void connect(final BluetoothDevice device) {
		/* Save connecting address */
		SharedPreferences sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(Constant.SharedPrefencesKeyword.PERIPHERAL_ADDRESS, device.getAddress());
		editor.commit();
		    		
		stopScan();
		
		/* Gatt connect() function must be called in UI thread */
		if (Looper.myLooper() == null) Looper.prepare();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		mainHandler.post(new Runnable() {
			
			@Override
			public void run() {
				mBleWrapper.connect(device.getAddress());
			}
		});
		
		if (mListener != null) {
			mListener.log("Connecting");
		}
	}
	
	public void disconnect() {
		mBleWrapper.diconnect();
	}
	
	public void send(byte[] data) {
		mBleWrapper.writeDataToCharacteristic(mWriteCharacter, data);
	}
	
	private void initBlewrapper() {
        mBleWrapper = new BleWrapper(getApplicationContext());
        mBleWrapper.setBLEWrapperListener(bleWrapperListener);
	}
	
	
	
	/**
	 * Sync device time with phone time
	 */
	 private void syncTime(){
			Calendar c = Calendar.getInstance(); 
			
			int second = c.get(Calendar.SECOND);
			int minute = c.get(Calendar.MINUTE);
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int date = c.get(Calendar.DAY_OF_MONTH);
			int month = c.get(Calendar.MONTH) + 1;
			int year = (c.get(Calendar.YEAR) - 2000) & 0xff;
			
			byte[] syncTimeData = new byte[]{'5', '5', '0', '0',  
											(byte)second, (byte)minute, 
											(byte)hour, (byte)date, 
											(byte)month, (byte)year};
			
			for (int i = 0; i < syncTimeData.length; i++) {
				Log.i(TAG, "" + syncTimeData[i]);
			}
			
			send(syncTimeData);
			
			mListener.log("sync time done");
	 }
	
	
	/**
	 * Build notification for make foregroundService.
	 * then service will not be killed when system low memory.
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

	    return(b.build());
	}
	
	/**
	 * Send password to device
	 */
	private void sendPassword() {
		byte data1[] = new byte[]{'5', '5', '0', '1', (byte)mPassword.length()};
		byte data2[] = mPassword.getBytes();
		byte data3[] = new byte[data1.length + data2.length];
		System.arraycopy(data1, 0, data3, 0, data1.length);
		System.arraycopy(data2, 0, data3, data1.length, data2.length);
	
		send(data3);
		
		if (mListener != null) {
            Log.i(TAG, "Sent password");
			mListener.log("sent password");
		}
	}
	
	/**
	 * FIXME
	 * Get name of scan device for advertisement.
	 * Android core can't read name.
	 * You can use another app on Google Play to verify
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
		} while(scanRecord[i] != 0);
		 
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
	private void save2SD(byte[] sData) {
		String sRoot = null;
		String sFileName = null;
		String sPath = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			sRoot = Environment.getExternalStorageDirectory().toString();
		else
			return;
	
		sFileName = year+"" + month + "" + date + 
				"" + hour + "" + min + "" + second + ".wav";
		sPath = sRoot.concat("/").concat(this.getString(R.string.app_name));
		if (covertByte2File(sPath, sFileName, sData)){
			String sMsg = ("save to:").concat(sPath).concat("/").concat(sFileName);
			if (mListener != null) {
				mListener.log(sMsg);				
			}
		} else {
			if (mListener != null) {
				mListener.log("Save fail");
			}
		}
	}

    /* Copy binary data into a file */
    private boolean covertByte2File(String sPath, String sFile, byte[] bData) {
        try	{
            File fhd = new File(sPath);
            if (!fhd.exists())
                if (!fhd.mkdirs())
                    return false;

            fhd = new File(sPath +"/"+ sFile);
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
        int temp = (int) (((data[8])&0xff) + ((data[9])&0xff)*(Math.pow(16, 2)) +
                ((data[10])&0xff)*(Math.pow(16, 4)) + ((data[11])&0xff)*(Math.pow(16, 6)));
		
        second = ((temp & 0x1f) * 2);
        temp = temp >> 5;

        min = temp & 0x3f;
        temp = temp  >> 6;

        hour = temp & 0x1f;
        temp = temp >> 5;

        date = temp & 0x1f;
        temp = temp >> 5;

        month = temp & 0xf;
        temp = temp >> 4;

        year = (temp & 0x7f) + 1980;

        if (mListener != null) {
			mListener.log(year + "-" + month + "-" +date + "_" + hour + ":" + min + ":" + second);
		}
	}
		
	private void getAudioFileSize(byte []data) {
		// Get audio length. Byte 4 to byte 7.
		dataRecvLength = (int) ((data[4]&0xff) + (data[5]&0xff)*(Math.pow(16, 2)) + 
				(data[6]&0xff)*(Math.pow(16, 4)) + (data[7]&0xff)*(Math.pow(16, 6)));
		if (mListener != null) {
			mListener.log("audio file size = " + dataRecvLength + " bytes");			
		}
		
		// Initialize receive buffer
		dataRecv = new byte[dataRecvLength];
	}
		
	private void readAudioHeader(byte[] header) {
		// DEBUG
		for (int i = 0; i < AUDIO_HEADER_SIZE; i++){
			Log.i(TAG, "" + header[i]);
		}
		
		getAudioFileSize(header);
		getAudioFileTimestamp(header);
	}
		
	private void receiveAudioFile(byte[] data) {
		if (dataRecvLength == 0) { 
			readAudioHeader(data);
			
			// Get start receive audio file  time
			startRecvTime = System.currentTimeMillis();
			
			isReceivingAudioFile = true;
			if (mListener != null) {
				mListener.log("Recieveing audio data...");				
			}
		} else if (recvCountAudio < dataRecvLength) {
			// Get audio data
			System.arraycopy(data, 0, dataRecv, recvCountAudio, data.length);
			recvCountAudio += data.length;
            Log.i(TAG, "recvCountAudio = " + recvCountAudio);

			/* End of audio file */
			if (recvCountAudio == dataRecvLength) {
                Log.i(TAG, "Receive done");
				// Save audio file to sdcard
				save2SD(dataRecv);
				
				// Calc transfer audio file time
				stopRecvTime = System.currentTimeMillis();
				long timeTransfer = (stopRecvTime - startRecvTime)/1000;// to second
				
				if (mListener != null) {
					mListener.log("Elapse " + timeTransfer + " seconds");
                    mListener.log("Speed  = " + (recvCountAudio/timeTransfer) + " B/s");
					mListener.log("Receive done");
				}

                // Clear variable, flags
                dataRecvLength = 0;
                recvCountAudio = 0;
                isReceivingAudioFile = false;
			} 
		}

		// Send ack is number bytes received.
		byte[] ack = new byte[]{(byte) ((data.length) & 0xff)};
		send(ack);
	}	
	
	/**
	 * Analyze received data
	 * 
	 * @param data
	 * @return 
	 * true: data is not audio
	 * false: data is audio
	 */
	private boolean readParameter(String str){
		 try {
			if (str.contains("RECLN")){
				String result = str.replace("RECLN", "");
				int x = Integer.parseInt(result);
				mParameterListener.onReceiveRECLN(x);
				return true;
			} else if (str.contains("PAUSELEVEL")){
				String result = str.replace("PAUSELEVEL", "");
				int x = Integer.parseInt(result);
				mParameterListener.onReceivePAUSELEVEL(x);
				return true;
			} 
			else if (str.contains("PAUSELEN")){
				String result = str.replace("PAUSELEN", "");
				int x = Integer.parseInt(result);
				mParameterListener.onReceivePAUSELEN(x);
				return true;
			}
			else if (str.contains("ACCELERSENS")){
				String result = str.replace("ACCELERSENS", "");
				int x = Integer.parseInt(result);
				mParameterListener.onReceiveACCELERSENS(x);
				return true;
			} 
			else if (str.contains("BTLOC")){
				String result = str.replace("BTLOC", "");
				int x = Integer.parseInt(result);
				mParameterListener.onRecieveBTLOC(x);
				return true;
			} 
			else if (str.contains("MIC")){
				String result = str.replace("MIC", "");
				int x = Integer.parseInt(result);
				mParameterListener.onRecieveMIC(x);
				return true;
			} 
			else if (str.contains("VERSION")){
				String result = str.replace("VERSION", "");
				mParameterListener.onReceiveVERSION(result);
				return true;
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
		 return false;
	 }
	
	private boolean checkPassword(String password) {
		if (password.equals("CONNECTED")) {
			if (mListener != null) {
				mListener.log("connected");
			}
			syncTime();
			return true;
		} 
		
		/* Not password value */
		return false;
	}
	
	
	/**
	 * Check receive data if we know it's not audio 
	 * 
	 * @return
	 * true: data is connection information or parameter
	 * false: data is audio header
	 */
	private boolean analyzeNotAudioFile(byte[] data) {
		String string = null;
		
		try {
			// Convert byte[] to String
			string = new String(data, "UTF-8");
			Log.i(TAG, "receive = " + string);
			
			// Check parameter
			String[] temp = string.split("5503");
			if (temp.length > 1) {
				if (readParameter(temp[1])) return true;
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
		return mBinder;
	}

	/************** BLEWrapper listener **************/
	BLEWrapperListener bleWrapperListener = new BLEWrapperListener() {
		
		@Override
		public void onReceiveData(BluetoothGatt gatt,
				BluetoothGattCharacteristic charac, byte[] data) {
			
		    if (!isReceivingAudioFile) {
		    	if (analyzeNotAudioFile(data)) return;
		    } 
		    	
		    receiveAudioFile(data);
		}
		
		@Override
		public void onListCharacteristics(List<BluetoothGattCharacteristic> chars) {
			for (BluetoothGattCharacteristic charac : chars) {
				Log.i(TAG, "characterestic = " + charac.getUuid());
				if (charac.getUuid().toString().equalsIgnoreCase(Constant.CHARACTERISTIC_UUID_TO_WRITE)) {
					mWriteCharacter = charac;
					mWriteCharacter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
				} else if (charac.getUuid().toString().equalsIgnoreCase(Constant.CHARACTERISTIC_UUID_TO_READ)) {
					mRecvCharacter = charac;
					mBleWrapper.setNotificationForCharacteristic(mRecvCharacter, true);
				}
			}
			
			if (mListener != null) {
				mListener.log("list charecterestic done");
			}
		}
		
		@Override
		public void onFoundService(List<BluetoothGattService> services) {
			for (BluetoothGattService service : services) {
				Log.i(TAG, "Service found = " + service.getUuid());
				if (service.getUuid().toString().equalsIgnoreCase(Constant.SERVICE_UUID)) {
					if (mListener != null) {
						mListener.log("list service done");
					}
					mBleWrapper.getCharacteristicsForService(service);
					break;
				}
			}
		}
		
		@Override
		public void onFoundDevice(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			 SharedPreferences sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
			 String uuid = sharedPref.getString(Constant.SharedPrefencesKeyword.PERIPHERAL_ADDRESS, null);
			
			 if (uuid == null) {	/* Phone didn't connect to any device before */
				 
				 // Already found out before
				 for (BluetoothDevice aDevice : mListDevice) { 
					 if (aDevice.equals(device)) return;
				 }
				 
				 /* Found new device */
				 mListDevice.add(device);
				 String name = getNameOfScanDevice(scanRecord);
				 mListener.onFoundNewDevice(device, name);
				 
			 } else if ((uuid != null) && 
					 (device.getAddress().toString().equalsIgnoreCase(uuid))) {  /* Device that connected before */
				 connect(device);
			 }
		}
		
		@Override
		public void onDisconnected() {
			recvCountAudio = 0;
			dataRecvLength = 0;
			isReceivingAudioFile = false;

            // Re-connect
            mBleWrapper.close();
            initBlewrapper();
            startScan();
		}
		
		@Override
		public void onConnectResult(ConnectState resultCode, String error) {
			if (resultCode == Constant.ConnectState.BLE_CONNECT_FAIL) {
				if (mListener != null) {
					mListener.log("Connect fail, error = " + error);
				}
				
				recvCountAudio = 0;
				dataRecvLength = 0;
				isReceivingAudioFile = false;

                if (!error.equals("null")) {
                    // Re-connect
                    mBleWrapper.close();
                    initBlewrapper();
                    startScan();
                }
	    	}
		}

		@Override
		public void onDidConfigureGatt() {
			sendPassword();
		}
	};
	
	public interface MyleServiceListener {
		public void onFoundNewDevice(BluetoothDevice device, String deviceName);
		public void log(String log);
	}
	
	public interface ParameterListener {
		public void onReceiveRECLN(int result);
		public void onReceivePAUSELEVEL(int result);
		public void onReceivePAUSELEN(int result);
		public void onRecieveBTLOC(int result);
		public void onReceiveVERSION(String version);
		public void onRecieveMIC(int result);
		public void onReceiveACCELERSENS(int result);
	}
}