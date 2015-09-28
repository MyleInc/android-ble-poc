package com.getmyle;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.getmyle.mylesdk.MyleService;
import com.getmyle.mylesdk.TapManager;

import java.util.Calendar;

/*
 * Display tasks log.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class LogActivity extends Activity implements
        TapManager.TapManagerListener, MyleService.MyleServiceListener {
    private static final String TAG = "LogActivity";

    public static final String INTENT_PARAM_UUID = "uuid";

    private TextView tvLog;
    private Menu mMenu;
    private String mChoosenDeviceUUID;
    private TapManager mTapManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        mChoosenDeviceUUID = getIntent().getStringExtra(INTENT_PARAM_UUID);

        // TextView
        tvLog = (TextView) findViewById(R.id.tv_log);

        // Setup actionbar
        getActionBar().setTitle(getResources().getString(R.string.log_ac_actionbar_title));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mTapManager = new TapManager(this);
        mTapManager.setTapManagerListener(this);

        mTapManager.connectToService();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mTapManager.stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mTapManager.destroy();
    }

    @Override
    public void onServiceConnected() {
        mTapManager.setMyleServiceListener(this);

        String password = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(AppConstants.PREF_PASSWORD, AppConstants.DEFAULT_PASSWORD);

        if (TextUtils.isEmpty(mChoosenDeviceUUID)) {
            mTapManager.startScan();
        } else {
            mTapManager.connectToDevice(mChoosenDeviceUUID, password);
        }
    }

    @Override
    public void onServiceDisconnected() {
        mTapManager.removeMyleServiceListener(this);
    }

    // Clear log
    public void clickClearLog() {
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
        switch (item.getItemId()) {

            case R.id.action_clear:
                clickClearLog();
                break;

            case R.id.action_parameter:
                startActivity(new Intent(this, ParameterActivity.class));
                break;

            case R.id.action_forget_device:
                mTapManager.forgetCurrentDevice();

                Toast.makeText(this, "Forgot current device", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_get_num_rev_audio:
                int num = mTapManager.getReceiveByteAudio();
                Toast.makeText(this, num + " bytes", Toast.LENGTH_LONG).show();
                break;

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFoundNewDevice(BluetoothDevice device, String deviceName) {
    }

    @Override
    public void log(String log) {
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int minute = c.get(Calendar.MINUTE);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        tvLog.append(hour + ":" + minute + ":" + seconds + ": " + log + "\n");
    }
}
