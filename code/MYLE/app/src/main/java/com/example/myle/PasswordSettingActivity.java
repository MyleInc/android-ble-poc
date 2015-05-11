package com.example.myle;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;

import com.example.myle.MyleService.LocalBinder;

/*
 * Show password.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class PasswordSettingActivity extends Activity{
	private static final String DEFAULT_PASSWORD = "1234abcd";
	private EditText mEdPassword;
	private MyleService mMyleService;
	private boolean mBounded;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_passwordsetting);
		getActionBar().setTitle(getResources().getString(R.string.passwordsetting_ac_actionbar_title));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Load old password 
		SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
		String password = prefs.getString(Constant.SharedPrefencesKeyword.PASSWORD, DEFAULT_PASSWORD);
		mEdPassword = (EditText)findViewById(R.id.ed_password);
		mEdPassword.setText(password);

        Intent mIntent = new Intent(this, MyleService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
	}
	
	 // Bound to SPPService
    @Override
	protected void onStart() {
      	super.onStart();
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
 		 }
 	};
 	
	public void clickStart(View v){
		// Save password
		mMyleService.setPassword(mEdPassword.getText().toString());
		
		// Goto scan/log activity
    	SharedPreferences sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		String address = sharedPref.getString(Constant.SharedPrefencesKeyword.PERIPHERAL_ADDRESS, null);
		
		if (address == null) {
			startActivity(new Intent(this, ScanActivity.class));
		} else {
			mMyleService.startScan();
			startActivity(new Intent(this, LogActivity.class));
		}
	}
}
