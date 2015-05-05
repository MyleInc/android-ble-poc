package com.example.myle;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/*
 * Start background service.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class LauncherActivity extends ActionBarActivity {
	private static final int REQUEST_ENABLE_BT = 1234;
	private Handler mHandler;
	private ServiceWaitThread mThread;
	private BluetoothAdapter mBluetoothAdapter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      //Check bluetooth is on
		 mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		   Toast.makeText(this, "Device doesn't support LE", Toast.LENGTH_LONG).show();
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			runService();
		}
    }

    private void runService() {
    	 mHandler = new Handler();

 		if (MyleService.isRunning()) {        
 			onServiceReady();
 		} else {
 			// start TCPService as background  
 			startService(new Intent(this, MyleService.class));
 			mThread = new ServiceWaitThread();
 			mThread.start();
 		}
    }
    
    protected void onServiceReady() {
		startActivity(new Intent(this, PasswordSettingActivity.class));
		 
		finish();
	}

	private class ServiceWaitThread extends Thread {
		public void run() {
			while (!MyleService.isRunning()) {
				try {
					sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException("waiting thread sleep() has been interrupted");
				}
			}

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onServiceReady();
				}
			});
			
			mThread = null;
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.launcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
   @Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
	   if (mBluetoothAdapter.isEnabled()) {
		   runService();
	   } else {
		   finish();
	   }
	   
	   super.onActivityResult(arg0, arg1, arg2);
	}

}
