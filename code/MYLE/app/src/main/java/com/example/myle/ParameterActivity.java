package com.example.myle;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myle.MyleService.LocalBinder;
import com.example.myle.MyleService.ParameterListener;

import java.util.Locale;

/*
 * Write & read parameters
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class ParameterActivity extends Activity {
	private EditText mEdRECLN, mEdPAUSELEVEL, 
		mEdPAUSELEN, mEdACCELERSENS,
		mEdMIC, mEdPASSWORD, 
		mEdBTLOC, mEdUUID, mEdVERSION;
	private MyleService mMyleService;
	private boolean mBounded;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_parameter);
		
		getActionBar().setTitle("Parameter");
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mEdRECLN = (EditText)findViewById(R.id.ed_recln);
		mEdPAUSELEVEL = (EditText)findViewById(R.id.ed_pause_level);
		mEdPAUSELEN = (EditText)findViewById(R.id.ed_pause_len);
		mEdACCELERSENS = (EditText)findViewById(R.id.ed_acceler_sens);
		mEdMIC = (EditText)findViewById(R.id.ed_mic);
		mEdPASSWORD = (EditText)findViewById(R.id.ed_password);
		mEdBTLOC = (EditText)findViewById(R.id.ed_btloc);
		mEdUUID = (EditText)findViewById(R.id.ed_uuid);
		mEdVERSION = (EditText)findViewById(R.id.ed_version);
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
   			
   			// Set listener
   			mMyleService.setParameterListener(mParameterListener);
   		 }
   	};
   	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		 case android.R.id.home:
		        NavUtils.navigateUpFromSameTask(this);
		        return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public void clickReadAll(View v) throws InterruptedException{
		// Read RECLN
		mMyleService.send(makeReadRECLN());
		Thread.sleep(100);
		
		// Read PAUSE_LEVEL
		mMyleService.send(makeReadPAUSELEVEL());
		Thread.sleep(100);
		
		// Read PAUSE_LEN
		mMyleService.send(makeReadPAUSELEN());
		Thread.sleep(100);
		
		// Read ACCELER_SENS
		mMyleService.send(makeReadACCELERSENS());
		Thread.sleep(100);
		
		// Read MIC
		mMyleService.send(makeReadMIC());
		Thread.sleep(100);
		
		// Read BTLOC
		mMyleService.send(makeReadBTLOC());
		Thread.sleep(100);
		
		// Read Version
		mMyleService.send(makeReadVERSION());
		Thread.sleep(100);
		
		// Read password
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// Set uuid
				mEdUUID.setText(Constant.SERVICE_UUID);
				
				// Set password 
				SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
				String password = prefs.getString(Constant.SharedPrefencesKeyword.PASSWORD, "1234abcd");
				mEdPASSWORD = (EditText)findViewById(R.id.ed_password);
				mEdPASSWORD.setText(password);
			}
		}, 250);
		
		Toast.makeText(this, "Read complete", Toast.LENGTH_LONG).show();
	}
	
	private byte[] makeReadRECLN(){
		byte[] a = "5503RECLN".getBytes();
		
		return a;
	}
	
	private byte[] makeReadBTLOC(){
		byte[] a = "5503BTLOC".getBytes();
		
		return a;
	}
	
	private byte[] makeReadPAUSELEVEL(){
		byte[] a = "5503PAUSELEVEL".getBytes();
		
		return a;
	}
	
	private byte[] makeReadPAUSELEN(){
		byte[] a = "5503PAUSELEN".getBytes();
		
		return a;
	}
	
	private byte[] makeReadACCELERSENS(){
		byte[] a = "5503ACCELERSENS".getBytes();
		
		return a;
	}
	
	private byte[] makeReadMIC(){
		byte[] a = "5503MIC".getBytes();
		
		return a;
	}
	
	private byte[] makeReadVERSION(){
		byte[] a = "5503VERSION".getBytes();
		
		return a;
	}
	
	public void clickWriteAll(View v) throws InterruptedException{
		// Write RECLN
		mMyleService.send(makeWriteRECLNCommand(mEdRECLN.getText().toString()));
		Thread.sleep(100);
		
		// Write PAUSE_LEVEL
		mMyleService.send(makeWritePAUSELEVELCommand(mEdPAUSELEVEL.getText().toString()));
		Thread.sleep(100);
		
		// Write PAUSE_LEN
		mMyleService.send(makeWritePAUSELENCommand(mEdPAUSELEN.getText().toString()));
		Thread.sleep(100);
		
		// Write ACCELER_SENS
		mMyleService.send(makeWriteACCELERSENSCommand(mEdACCELERSENS.getText().toString()));
		Thread.sleep(100);
		
		// Write MIC
		mMyleService.send(makeWriteMICCommand(mEdMIC.getText().toString()));
		Thread.sleep(100);
		
		// Write BTLOC
		mMyleService.send(makeWriteBTLOCCommand(mEdBTLOC.getText().toString()));
		Thread.sleep(100);
		
		// Write PASSWORD
		mMyleService.send(makeWritePASSWORDCommand(mEdPASSWORD.getText().toString()));
		SharedPreferences sharedPref1 = getSharedPreferences("password", MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref1.edit();
		editor.putString("PASSWORD", mEdPASSWORD.getText().toString());
		editor.commit();
		
		Toast.makeText(this, "Write complete", Toast.LENGTH_LONG).show();
	}
	
	private byte[] makeWriteRECLNCommand(String value) {
		String temp = String.format(Locale.getDefault(), "%02d", Integer.parseInt(value));
		String str = "5502RECLN" + temp;
		Log.i("", str);
		byte[] c = str.getBytes();
		return c;
	}
	
	private byte[] makeWritePAUSELEVELCommand(String value){
		String temp = String.format(Locale.getDefault(), "%03d", Integer.parseInt(value));
		String str = "5502PAUSELEVEL" + temp;
		byte[] c = str.getBytes();
		return c;
	}
	
	private byte[] makeWritePAUSELENCommand(String value){
		String temp = String.format(Locale.getDefault(), "%02d", Integer.parseInt(value));
		String str = "5502PAUSELEN" + temp;
		byte[] c = str.getBytes();
		return c;
	}
	
	private byte[] makeWriteACCELERSENSCommand(String value){
		String temp = String.format(Locale.getDefault(), "%03d", Integer.parseInt(value));
		String str = "5502ACCELERSENS" + temp;
		byte[] c = str.getBytes();
		return c;
	}
	
	private byte[] makeWriteMICCommand(String value){
		String temp = String.format(Locale.getDefault(), "%03d", Integer.parseInt(value));
		String str = "5502MIC" + temp;
		byte[] c = str.getBytes();
		return c;
	}
	
	private byte[] makeWritePASSWORDCommand(String value){
        byte[] a = new byte[]{'5', '5', '0', '2','P', 'A', 'S', 'S', (byte)value.length()};
        byte[] b = value.getBytes();

        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
	}
	
	private byte[] makeWriteBTLOCCommand(String value){
		String temp = "5502BTLOC" + value;
		byte[] c = temp.getBytes();
		return c;
	}

	MyleService.ParameterListener mParameterListener = new ParameterListener() {
		
		@Override
		public void onReceiveRECLN(final int result) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdRECLN.setText(result + "");				
				}
			});
		}
		
		@Override
		public void onReceivePAUSELEVEL(final int result) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdPAUSELEVEL.setText(result + "");				
				}
			});
		}

		@Override
		public void onReceivePAUSELEN(final int result) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdPAUSELEN.setText(result + "");				
				}
			});
		}

		@Override
		public void onReceiveACCELERSENS(final int result) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdACCELERSENS.setText(result + "");				
				}
			});
		}
		
		@Override
		public void onRecieveBTLOC(final int result) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdBTLOC.setText(result + "");				
				}
			});
		}

		@Override
		public void onReceiveVERSION(final String version) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdVERSION.setText(version);				
				}
			});
		}

		@Override
		public void onRecieveMIC(final int result) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mEdMIC.setText(result + "");				
				}
			});
		}

	}; 
	
	public void clickloct(View v){
		mMyleService.send("loct".getBytes());
	}
}
