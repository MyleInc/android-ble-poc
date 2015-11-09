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
import android.widget.Toast;

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

    private ListView mListview;
    private ScanAdapter mAdapter;
    private ArrayList<MyleDevice> mListDevice = new ArrayList<>();

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshList();
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

        mListview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (mAdapter.getItem(position).getDevice() == TapManager.getInstance().getConnectedTap()) {
                    Toast.makeText(ScanActivity.this, "Already connected", Toast.LENGTH_LONG).show();
                    return;
                }

                String tapAddress = mAdapter.getItem(position).getDevice().getAddress();

                PreferenceManager.getDefaultSharedPreferences(ScanActivity.this)
                        .edit()
                        .putString(AppConstants.PREF_ADDRESS, tapAddress)
                        .apply();

                Intent intent = new Intent(ScanActivity.this, PasswordSettingActivity.class);
                intent.putExtra(PasswordSettingActivity.INTENT_PARAM_UUID, tapAddress);
                startActivity(intent);
                finish();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        refreshList();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Constant.TAP_NOTIFICATION_SCAN));
    }


    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }


    private void refreshList() {
        Collection<BluetoothDevice> taps = TapManager.getInstance().getAvailableTaps();
        mListDevice.clear();
        BluetoothDevice connectedTap = TapManager.getInstance().getConnectedTap();
        for (BluetoothDevice tap : taps) {
            String name = TapManager.getInstance().getTapName(tap.getAddress()) + ((connectedTap == tap) ? " (connected)" : "");
            mListDevice.add(new MyleDevice(tap, name));
        }
        mAdapter.notifyDataSetChanged();
    }

}
