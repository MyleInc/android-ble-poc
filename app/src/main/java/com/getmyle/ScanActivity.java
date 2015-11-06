package com.getmyle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.getmyle.mylesdk.Constant;
import com.getmyle.mylesdk.TapManager;

import java.util.ArrayList;
import java.util.Collection;

/*
 * Display scan ble device result.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class ScanActivity extends Activity {
    private static final String TAG = "ScanActivity";
    private static final int REQUEST_ENABLE_BT = 1234;

    private ListView mListview;
    private ScanAdapter mAdapter;
    private ArrayList<MyleDevice> mListDevice = new ArrayList<>();

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Collection<BluetoothDevice> taps = TapManager.getInstance().getAvailableTaps();
            mListDevice.clear();
            for (BluetoothDevice tap : taps) {
                mListDevice.add(new MyleDevice(tap, TapManager.getInstance().getTapName(tap.getAddress())));
            }
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        getActionBar().setTitle(getResources().getString(R.string.scan_ac_actionbar_title));

        mListview = (ListView) findViewById(R.id.lv_scan_device);
        mAdapter = new ScanAdapter(this, R.layout.scan_device_item, mListDevice);
        mListview.setAdapter(mAdapter);

        final Activity me = this;

        mListview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                String tapAddress = mAdapter.getItem(position).getDevice().getAddress();

                PreferenceManager.getDefaultSharedPreferences(me)
                        .edit()
                        .putString(AppConstants.PREF_ADDRESS, tapAddress)
                        .apply();

                Intent intent = new Intent(ScanActivity.this, PasswordSettingActivity.class);
                intent.putExtra(PasswordSettingActivity.INTENT_PARAM_UUID, tapAddress);
                startActivity(intent);
                finish();
            }
        });

        //Check bluetooth is on
        // TODO: move this check to tap manager?
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, "Device doesn't support BLE", Toast.LENGTH_LONG).show();
//        }
//
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Constant.TAP_NOTIFICATION_SCAN));
    }


    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan, menu);

        updateScanMenuItem(menu.getItem(0));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_scan) {
            if (TapManager.getInstance().isScanning()) {
                TapManager.getInstance().stopScan();
            } else {
                TapManager.getInstance().startScan();
            }

            updateScanMenuItem(item);

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void updateScanMenuItem(MenuItem menuItem) {
        if (TapManager.getInstance().isScanning()) {
            menuItem.setTitle(R.string.scan_ac_stop_scan);
        } else {
            menuItem.setTitle(R.string.scan_ac_start_scan);
        }
    }

}
