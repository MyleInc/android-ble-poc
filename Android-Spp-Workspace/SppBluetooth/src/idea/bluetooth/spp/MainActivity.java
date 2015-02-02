package idea.bluetooth.spp;

import idea.bluetooth.spp.SPPService.LocalBinder;
import idea.bluetooth.spp.util.LocalIOTools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author cxphong
 */

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	private TextView tvLog;
	private SPPService mSPPService;
	private boolean mBounded;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // TextView
        tvLog = (TextView)findViewById(R.id.tv_log);
        
        // Register @mMessageReceiver broadcast for listen message from @SPPService
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
        	      new IntentFilter(Constant.SPP_SERVICE_INTENT));
        
        // Run SppService
    	Intent intent = new Intent(this, SPPService.class);
        startService(intent);
    }
   
    // Bound to SPPService
    @Override
	protected void onStart() {
		super.onStart();
		Intent mIntent = new Intent(this, SPPService.class);
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
    	super.onDestroy();
    	// Unregister @mMessageReceiver broadcastReceiver 
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }
    
    // BroadcastReceiver for listen SPPService message
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    	    String logMessage = intent.getStringExtra(Constant.SPP_SERVICE_INTENT_LOG);
    	    String totalRxMessage = intent.getStringExtra(Constant.SPP_SERVICE_INTENT_TOTAL_RX);
    	    
    	    if (logMessage != null)
    	    	tvLog.append(logMessage + "\n");
    	  }
    	};
    
    // Handle connection service
	ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			mBounded = false;
			mSPPService = null;
	  	}

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBounded = true;
			LocalBinder mLocalBinder = (LocalBinder)service;
			mSPPService = mLocalBinder.getServerInstance();
		 }
	};

	// disconnect BluetoothSSPP 
	public void clickStartOrStopSPPService(View v){
	}
	
	// Get total number bytes had received
	public void clickGetTotalRx(View v){
		int num = mSPPService.getTotalRx();
		final DialogNotification dialog = DialogNotification.newInstance(num + " byte", "RX");
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				dialog.show(getFragmentManager(), null);				
			}
		});
	}
	
	// Save data receive to sdcard
	public void clickSave2SDCard(View v){
		String data = mSPPService.getRexData();
		save2SD(data);
	}
	
	// Save file into sdcard
	protected void save2SD(String sData){
		String sRoot = null;
		String sFileName = null;
		String sPath = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			sRoot = Environment.getExternalStorageDirectory().toString();
		else
			return;
	
		sFileName = (new SimpleDateFormat("MMddHHmmss", Locale.getDefault())).format(new Date()) + ".txt";
		sPath = sRoot.concat("/").concat(this.getString(R.string.app_name));
		if (LocalIOTools.coverByte2File(sPath, sFileName, sData.getBytes())){
			String sMsg = ("save to:").concat(sPath).concat("/").concat(sFileName);
			Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(this, "Save fail",
			   Toast.LENGTH_SHORT).show();
		}
	}
	
	// Clear log
	public void clickClearLog(){
		tvLog.setText("");
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        	
        case R.id.action_start_stop:
        	if (mSPPService.isRunning()) {
    			if(mBounded) {
    		 		   unbindService(mConnection);
    		 		   mBounded = false;
    		 		 }
    			
    			Intent intent = new Intent(MainActivity.this, SPPService.class);
    	        stopService(intent);
    	        tvLog.append("Stop task\n");
    	        item.setTitle(getResources().getString(R.string.main_ac_start_spp));
    		} else {
    			Intent intent = new Intent(MainActivity.this, SPPService.class);
    	        startService(intent);
    	        Intent mIntent = new Intent(this, SPPService.class);
    	        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    	        item.setTitle(getResources().getString(R.string.main_ac_stop_spp));
    		}
        	break;
        	
        case R.id.action_location:
        	byte[] data = "loct".getBytes();
        	mSPPService.send(data);
        	break;
        
        case R.id.action_en_bloc:
        	byte[] data1 = "bloc1".getBytes();
        	mSPPService.send(data1);
        	break;
        	
        case R.id.action_dis_bloc:
        	byte[] data2 = "bloc0".getBytes();
        	mSPPService.send(data2);
        	break;
        }
        
        
        return super.onOptionsItemSelected(item);
    }
}