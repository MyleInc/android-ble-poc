package idea.bluetooth.spp;

import idea.bluetooth.spp.library.BluetoothSPP;
import idea.bluetooth.spp.library.BluetoothSPPListener;
import idea.bluetooth.spp.util.LocalIOTools;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * SPP manager
 * @author cxphong
 */

public class SPPService extends Service implements BluetoothSPPListener{
	private static final String TAG = "SPPService";
	
	private BluetoothSPP bt;
	
	// Spp device MAC address
	private String DEVICE_ADDRESS = "2C:F7:F1:81:02:25";
	
	// String will to save into sdcard
	private String dataString = "";
	
	private final IBinder mBinder = new LocalBinder();
	
	// static due to service keep running in background
	private static boolean isRunning;
	
	// Flag receive thread
	private boolean mbThreadStop;
	
	// Header size
	private int audioHeaderSize = 12;
	
	// data byte[]
	private byte[] dataRecv;
	private int dataRecvLength;
	private int recvCountAudio;
	private int recvCountTotal;
	
	int second, min, hour, date, month, year;
	
	long startRecvTime, stopRecvTime;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		  public SPPService getServerInstance() {
			  return SPPService.this;
		  }
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (isRunning) {
			Log.d(TAG, "Already running");
			return;
		}
		Log.d(TAG, "Start service");
		isRunning = true;
		
		bt = new BluetoothSPP(this);
		bt.setBluetoothSPPListener(this);
		 
		// Check if device supports bluetooth spp
        if(!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        
        // Enable bluetooth & start spp jobs
        if(!bt.isBluetoothEnabled()) {
            bt.enable();
        }
        
        // Start scan
        sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Start discovery ...");
        bt.startDiscoverry();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mbThreadStop = true;
		bt.stopDiscovery();
		bt.disconnect();
		isRunning = false;
		Log.i(TAG, "onDestroy");
	}
	
	public boolean isRunning(){
		return isRunning;
	}
	
	public void send(byte[] data){
		bt.SendData(data);
	}
	
	// Get number of bytes that app had received
	public int getTotalRx(){
		return bt.getTotalRx();
	}
	
	// Get data had received
	public String getRexData(){
		String ret = dataString;
		dataString = "";
		return ret;
	}
	
	@Override
	public void onFoundDevice(BluetoothDevice device) {
		if (device.getAddress().equals(DEVICE_ADDRESS)) {
			sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Found device " + device.getName());
			bt.stopDiscovery();
			
			// Connect device
			sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Connecting");
			if (bt.connect(device) == BluetoothSPP.CONNECT_FAILED) {
				sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Connect fail");
				
				// Connect failed, start discovery again
				bt.startDiscoverry();
			} else {
				// sync time
				Calendar c = Calendar.getInstance(); 
				int second = c.get(Calendar.SECOND);
				int minute = c.get(Calendar.MINUTE);
				int hour = c.get(Calendar.HOUR_OF_DAY);
				int date = c.get(Calendar.DAY_OF_MONTH);
				int month = c.get(Calendar.MONTH) + 1;
				int year = c.get(Calendar.YEAR) - 2000;
				byte[] syncTimeData = new byte[]{("t".getBytes())[0], ("i".getBytes())[0], 
						("m".getBytes())[0], ("e".getBytes())[0], 
						(byte)second, (byte)minute, (byte)hour, (byte)date, (byte)month, (byte)year};
				
				bt.SendData(syncTimeData);
				
				
				sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Connected");
				bt.startReceiveData();
				
				//Start receiving thread
				new receiveTask().executeOnExecutor((ExecutorService) Executors.newCachedThreadPool());
				
				// Test send 
				byte data[] = {(byte) 0x32};
				if (bt.SendData(data) == data.length){
					//sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Send success ...");
				} else {
					//sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Send fail ...");
				}
			}
		}
		
	}

	private void sendBroadcastMessage(String tag, String message){
		Intent intent = new Intent(Constant.SPP_SERVICE_INTENT);
		intent.putExtra(tag, message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	public void onReceiveData() {
	}
	
	//sync time   "time"+date-time(time) 
	//enable location "bloc"+ '1' enable/ '0' disable
	//location "loct"
	//transfer successful "file" + '1' successfull/ '0' fail
	 private class receiveTask extends AsyncTask<String, byte[], Integer>
	    {
	    	/**Constant: the connection is lost*/
	    	private final static byte CONNECT_LOST = 0x01;
	    	/**Constant: the end of the thread task*/
	    	private final static byte THREAD_END = 0x02;
	    	
			@Override
			public void onPreExecute()
			{
				mbThreadStop = false;
				dataRecvLength = 0;
				recvCountAudio = 0;
				recvCountTotal = 0;
			}
			
			@Override
			protected Integer doInBackground(String... arg0){
				while(!mbThreadStop){
					if (bt.getReceiveBufLen() > 0){
						SystemClock.sleep(20); //先延迟让缓冲区填满
						byte[] btTmp = bt.getData();
						
						if (recvCountTotal == 0){
							for (int i = 0; i < audioHeaderSize; i++){
								Log.i(TAG, "" + btTmp[i]);
							}
							
							// Get cstart receive audio file  time
							startRecvTime = System.currentTimeMillis();
							
							// Get audio length
							dataRecvLength = (int) ((btTmp[4]&0xff) + (btTmp[5]&0xff)*(Math.pow(16, 2)) + 
									(btTmp[6]&0xff)*(Math.pow(16, 4)) + (btTmp[7]&0xff)*(Math.pow(16, 6)));
							Log.i(TAG, "dataRecvLength = " + dataRecvLength);
							dataRecv = new byte[dataRecvLength];
							
							// get date time
							int temp = (int) ((btTmp[8]) + (btTmp[9])*(Math.pow(16, 2)) + 
									(btTmp[10])*(Math.pow(16, 4)) + (btTmp[11])*(Math.pow(16, 6)));
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
							
							year = (temp & 0x7f) + 2000;
							
							Log.i(TAG, "" + second + ", " + min + ", " + 
							hour + ", " + date + ", " + month + ", " + year);
							
							// Get audio data
							if(btTmp.length > audioHeaderSize){
								for (int i = 0; i < btTmp.length - audioHeaderSize; i++){
									dataRecv[recvCountAudio + i] = btTmp[i + audioHeaderSize];
								}
								recvCountAudio += btTmp.length  - audioHeaderSize;
							}
							
							sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Start receive audio file ...");
							
							recvCountTotal += btTmp.length;
						} else if(dataRecvLength > recvCountAudio){
							// Get audio data
							for (int i = 0; i < btTmp.length; i++){
								dataRecv[recvCountAudio + i] = btTmp[i];
							}
							recvCountAudio += btTmp.length; 
							recvCountTotal += btTmp.length;

							if (dataRecvLength == recvCountAudio){
								Log.i(TAG, "Receive full");
								
								// Calc transfer audio file time
								stopRecvTime = System.currentTimeMillis();
								long timeTransfer = (stopRecvTime - startRecvTime)/1000;// to second
								sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Eslapse " + timeTransfer + " seconds");
								
								// Save data
								save2SD(dataRecv);
								
								sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, "Receive done");
								
								// clear receive buffer
								bt.clearDataBuffer();
								dataRecvLength = 0;
								recvCountAudio = 0;
								recvCountTotal = 0;
								
								// Send success ACK
								bt.SendData("file1".getBytes());
							}
						}
					}
				}
				return (int)THREAD_END;
			}
			
			@Override
			public void onProgressUpdate(byte[]... progress){
			}
			
			@Override
			public void onPostExecute(Integer result){
				dataRecvLength = 0;
				recvCountAudio = 0;
				recvCountTotal = 0;
			}
	    }
	 
	// Save file into sdcard
		protected void save2SD(byte[] sData){
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
			if (LocalIOTools.coverByte2File(sPath, sFileName, sData)){
				String sMsg = ("save to:").concat(sPath).concat("/").concat(sFileName);
				sendBroadcastMessage(Constant.SPP_SERVICE_INTENT_LOG, sMsg);
				Log.i(TAG, "" + sMsg);
			}else{
				Toast.makeText(this, "Save fail",
				   Toast.LENGTH_SHORT).show();
			}
		}
}


