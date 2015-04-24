package com.example.myle;

import java.util.Calendar;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myle.MyleService.LocalBinder;
import com.example.myle.MyleService.MyleServiceListener;

/*
 * Display tasks log.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class LogActivity extends Activity{
	private static final String TAG = "LogActivity";
	private TextView tvLog;
	private MyleService mMyleService;
	private boolean mBounded;
	private Menu mMenu;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        
        // TextView
        tvLog = (TextView)findViewById(R.id.tv_log);
        
        // Setup actionbar
        getActionBar().setTitle(getResources().getString(R.string.log_ac_actionbar_title));
		getActionBar().setDisplayHomeAsUpEnabled(true);
    }
	   
    // Bound to SPPService
    @Override
	protected void onStart() {
      	super.onStart();
      	
    	Intent mIntent = new Intent(this, MyleService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
	}
	    
    @Override
    protected void onStop() {
    	super.onStop();
    	
    }
	    
    @Override
    protected void onDestroy() {
    	super.onDestroy();

    	mMyleService.stopScan();
    	mMyleService.disconnect();
    	
    	// Unbound service
		if(mBounded) {
 		   unbindService(mConnection);
 		   mBounded = false;
		}
    }
	    
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
			
			mMyleService.setMyleServiceListener(listener);
		 }
	};

	// Clear log
	public void clickClearLog(){
		tvLog.setText("");
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
        
        case R.id.action_clear:
        	clickClearLog();
        	break;
        	
        case R.id.action_parameter:
        	startActivity(new Intent(this, ParameterActivity.class));
        	break;
        	
        case R.id.action_forget_device:
    		SharedPreferences sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
    		SharedPreferences.Editor editor = sharedPref.edit();
    		editor.putString(Constant.SharedPrefencesKeyword.PERIPHERAL_ADDRESS, null);
    		editor.commit();
    		
    		Toast.makeText(this, "Forgot current device", Toast.LENGTH_LONG).show();
    		break;
    		
        case android.R.id.home:
	        NavUtils.navigateUpFromSameTask(this);
	        finish();
	        return true;
        }
        
        
        return super.onOptionsItemSelected(item);
    }
	    
	MyleServiceListener listener = new MyleServiceListener() {
		
		@Override
		public void onFoundNewDevice(BluetoothDevice device, String deviceName) {
		}
		
		@Override
		public void log(final String log) {
			runOnUiThread(new Runnable() {
				public void run() {
					Calendar c = Calendar.getInstance(); 
					int seconds = c.get(Calendar.SECOND);
					int minute = c.get(Calendar.MINUTE);
					int hour = c.get(Calendar.HOUR_OF_DAY);
					
					tvLog.append(hour + ":" + minute + ":" + seconds + ": " + log + "\n");
				}
			});
		}
	};
}
