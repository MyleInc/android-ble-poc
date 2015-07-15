package com.example.myle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.example.myle.MyleService.LocalBinder;
import com.example.myle.MyleService.MyleServiceListener;

import java.util.ArrayList;

/*
 * Display scan ble device result.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class ScanActivity extends Activity {
	private static final String TAG = "ScanActivity";
    private static final int REQUEST_ENABLE_BT = 1234;
	private MyleService mMyleService;
	private boolean mBounded;
	private ListView mListview;
	private ScanAdapter mAdapter;
	private boolean mIsScanning;
	private ArrayList<MyleDevice> mListDevice = new ArrayList<MyleDevice>();
    private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		
		getActionBar().setTitle(getResources().getString(R.string.scan_ac_actionbar_title));
		
		mListview = (ListView)findViewById(R.id.lv_scan_device);
		mAdapter = new ScanAdapter(this, R.layout.scan_device_item, mListDevice);
		mListview.setAdapter(mAdapter);
		mListview.setOnItemClickListener(listener);

        //Check bluetooth is on
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support LE", Toast.LENGTH_LONG).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Intent mIntent = new Intent(this, MyleService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		mListDevice.clear();
		mAdapter.notifyDataSetChanged();
		mMyleService.stopScan();

        // Unbound service
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

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
        		mAdapter.notifyDataSetChanged();
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
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
    		Intent intent = new Intent(ScanActivity.this, LogActivity.class);
            intent.putExtra("DEVICE_INDEX", position);
    		startActivity(intent);
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

    @Override
    protected void onActivityResult(int arg0, int arg1, Intent arg2) {
        if (!mBluetoothAdapter.isEnabled()) {
            finish();
        }

        super.onActivityResult(arg0, arg1, arg2);
    }
}
