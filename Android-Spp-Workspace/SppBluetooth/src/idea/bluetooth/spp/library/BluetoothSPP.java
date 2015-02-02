package idea.bluetooth.spp.library;

import idea.bluetooth.spp.util.CResourcePV;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

/**
 * @date 16, Jan, 2015
 * @author cxphong
 */
public class BluetoothSPP {
	private static final String TAG = "BlueToothSPP";
	
	public static final int CONNECT_SUCCESS = 0x00;
	
	public static final int CONNECT_FAILED = 0x01;
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private Context mContext;
	
	private BluetoothSPPListener mBluetoothSPPListener;
	
	private BluetoothSocket mbsSocket = null;
	
	private InputStream misIn = null;
	
	private OutputStream mosOut = null;
	
	// UUID for connect. It is constant for all device
	public final static String UUID_SPP = "00001101-0000-1000-8000-00805F9B34FB";
	
	private boolean isStopDiscovery;
	
	private boolean isConnecting;
	
	private int numRecv;
	
	// A fifo to save received data
	private	BlockingQueue<byte[]> fifo = new LinkedBlockingQueue<byte[]>();
	
	private static final int iBUF_TOTAL = 1024 * 300;
	
	private final byte[] mbReceiveBufs = new byte[iBUF_TOTAL];
	
	private int miBufDataSite = 0;
	
	private final CResourcePV mresReceiveBuf = new CResourcePV(1);
	
	public BluetoothSPP(Context context) {
		this.mContext = context;
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	// Check if phone supports bluetooth
	public boolean isBluetoothAvailable() {
        try {
            if (mBluetoothAdapter == null || mBluetoothAdapter.getAddress().equals(null))
                return false;
        } catch (NullPointerException e) {
             return false;
        }
        return true;
    }
	
	// Check if bluetooth is enable
	public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }
	
	// Enable bluetooth
	public void enable() {
        mBluetoothAdapter.enable();
    }
	
	// Start scan
	public void startDiscoverry() {
		// Register broadcast receiver to listen scan message
		IntentFilter bluetoothFilter = new IntentFilter();
		bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND);
		bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.mContext.registerReceiver(this.discoveryBroadcast, bluetoothFilter);
		
		// Start scan
		this.mBluetoothAdapter.startDiscovery();
		isStopDiscovery = false;
	}

	// Stop scan
	public void stopDiscovery() {
		if (!isStopDiscovery){
			this.mBluetoothAdapter.cancelDiscovery();
			isStopDiscovery = true;
			mContext.unregisterReceiver(discoveryBroadcast);
		}
	}
	
	// Listen bluetooth discovery message
	// Discovery run with period is 12s
	// Finish, if found device then stop discovery. Otherwise, continue discovery
	private BroadcastReceiver discoveryBroadcast  = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
				Log.i(TAG, "Started discovery");
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				Log.i(TAG, "Found device");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (mBluetoothSPPListener != null) {
					mBluetoothSPPListener.onFoundDevice(device);
				}
			} else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				Log.d(TAG, "Bluetooth scanning is finished");
				BluetoothSPP.this.mBluetoothAdapter.startDiscovery();
			} 
		}
		
	};
	
	// Set BluetoothSPPListener
	public void setBluetoothSPPListener(BluetoothSPPListener listener) {
		this.mBluetoothSPPListener = listener;
	}
	
	// Connect to device
	@SuppressLint("NewApi") public int connect(BluetoothDevice device){
		final UUID uuidSPP = UUID.fromString(BluetoothSPP.UUID_SPP);
		try{
			if (Build.VERSION.SDK_INT >= 10)
				this.mbsSocket = device.createInsecureRfcommSocketToServiceRecord(uuidSPP);
			else
				this.mbsSocket = device.createRfcommSocketToServiceRecord(uuidSPP);
			
			this.mbsSocket.connect();
			this.mosOut = this.mbsSocket.getOutputStream();
			this.misIn = this.mbsSocket.getInputStream(); 
		}catch (IOException e){
			e.printStackTrace();
			return CONNECT_FAILED;
		}finally{
		} 
		
		isConnecting = true;
		return CONNECT_SUCCESS;
	}
	
	
	// Disconnect device
	public void disconnect() {
		if (this.mbsSocket != null) {
			try {
				mbsSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				isConnecting = false;
			}
		}
	}
	
	// Send data 
	public int SendData(byte[] btData){
		if (this.isConnecting){
			try{
				mosOut.write(btData);
				return btData.length;
			}catch (IOException e){
				this.disconnect();
				return -1;
			}
		}
		else
			return -1;
	}
	
	// Receive data, run asynctask ReceiveThread
	public void startReceiveData(){
		new ReceiveThread().execute("");
	}
	
	public int getTotalRx(){
		return numRecv;
	}
	
	// get size of mbReceiveBufs
	public int getReceiveBufLen(){
		int iBufSize = 0;
		this.P(this.mresReceiveBuf);
		iBufSize = this.miBufDataSite;
		this.V(this.mresReceiveBuf);
		return iBufSize;
	}
	
	private void P(CResourcePV res){
		while(!res.seizeRes())
			SystemClock.sleep(2);
	}

	private void V(CResourcePV res){
		res.revert();
	}
	
	public byte[] getData(){
		byte[] btBufs = null;
		
		this.P(this.mresReceiveBuf);
		if (this.miBufDataSite > 0){
			btBufs = new byte[this.miBufDataSite];
			for(int i=0; i<this.miBufDataSite; i++)
				btBufs[i] = this.mbReceiveBufs[i];
			this.miBufDataSite = 0;
		}
		this.V(this.mresReceiveBuf);
		
		return btBufs;
	}
	
	// Clear buffer for another file
	public void clearDataBuffer(){
		numRecv = 0;
		miBufDataSite = 0;
	}
	
	class ReceiveThread extends AsyncTask<String, String, Integer>{
		// Device in this app send each 50 bytes at a time 
		static private final int BUFF_MAX_CONUT = 50;
		static private final int CONNECT_LOST = 0x01;
		static private final int THREAD_END = 0x02;

		@Override
		public void onPreExecute(){
			numRecv = 0;
		}

		@Override
		protected Integer doInBackground(String... arg0){
			Log.i(TAG, "doInBackground");
			int iReadCnt = 0;
			byte[] btButTmp = new byte[BUFF_MAX_CONUT]; 

			while(isConnecting){
				try {
					iReadCnt = misIn.read(btButTmp);
				
					P(mresReceiveBuf);
					numRecv += iReadCnt;
					if ( (miBufDataSite + iReadCnt) > iBUF_TOTAL)
						miBufDataSite = 0;
					for(int i=0; i<iReadCnt; i++)
						mbReceiveBufs[miBufDataSite + i] = btButTmp[i];
					miBufDataSite += iReadCnt;
					V(mresReceiveBuf);
				}catch (IOException e){
					return CONNECT_LOST;
				}
			}
			return THREAD_END;
		}

		@Override
		public void onPostExecute(Integer result){
			if (CONNECT_LOST == result){
				Log.i(TAG, "Lost connection");
				disconnect();
			}else{
				try{
					misIn.close();
					misIn = null;
					numRecv = 0;
				}catch (IOException e){
					misIn = null;
				}
			}
		}
	}

}