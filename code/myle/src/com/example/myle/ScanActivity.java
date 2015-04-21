package com.example.myle;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.example.myle.MyleService.LocalBinder;
import com.example.myle.MyleService.MyleServiceListener;

/*
 * Display scan ble device result.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class ScanActivity extends Activity {
	private static final String TAG = "ScanActivity";
	private MyleService mMyleService;
	private boolean mBounded;
	private ListView mListview;
	private ScanAdapter mAdapter;
	private boolean mIsScanning;
	private ArrayList<MyleDevice> mListDevice = new ArrayList<MyleDevice>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		
		getActionBar().setTitle(getResources().getString(R.string.scan_ac_actionbar_title));
		
		mListview = (ListView)findViewById(R.id.lv_scan_device);
		mAdapter = new ScanAdapter(this, R.layout.scan_device_item, mListDevice);
		mListview.setAdapter(mAdapter);
		mListview.setOnItemClickListener(listener);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Intent mIntent = new Intent(this, MyleService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
	}
	
	@Override
    protected void onStop() {
    	super.onStop();
    	
    	// Unbound service
		if(mBounded) {
 		   unbindService(mConnection);
 		   mBounded = false;
		}
    }

	@Override
	protected void onDestroy() {
		mListDevice.clear();
		mMyleService.stopScan();
		Intent intent = new Intent(ScanActivity.this, MyleService.class);
		stopService(intent);

		super.onDestroy();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
        
        	
        case R.id.action_scan:
        	if (mIsScanning) {
        		mMyleService.stopScan();
        		item.setTitle(R.string.scan_ac_start_scan);
        		mListDevice.clear();
        		mIsScanning = false;
        	} else {
        		mMyleService.startScan();
        		item.setTitle(R.string.scan_ac_stop_scan);
        		mIsScanning = true;
        	}
        	
	        return true;
        }
        
        
        return super.onOptionsItemSelected(item);
    }
    
	AdapterView.OnItemClickListener listener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
    		// Connect to device
    		mMyleService.connect(mListDevice.get(position).getDevice());
    		startActivity(new Intent(ScanActivity.this, LogActivity.class));
    		finish();
		}
	};
	
	// Handle connection service
	ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			mBounded = false;
			mMyleService = null;
	  	}

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBounded = true;
			LocalBinder mLocalBinder = (LocalBinder)service;
			mMyleService = mLocalBinder.getServerInstance();
			
			mMyleService.setMyleServiceListener(myleServiceListener);
			mMyleService.startScan();
			mIsScanning = true;
		 }
	};

	MyleServiceListener myleServiceListener = new MyleServiceListener() {
		
		@Override
		public void onFoundNewDevice(final BluetoothDevice device, final String deviceName) {
			Log.i(TAG, "onFoundNewDevice " + device);
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mListDevice.add(new MyleDevice(device, deviceName));
					mAdapter.notifyDataSetChanged();
				}
			});			
		}

		@Override
		public void log(String log) {
		}
	};
}
