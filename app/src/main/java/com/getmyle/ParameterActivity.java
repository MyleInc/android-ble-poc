package com.getmyle;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.getmyle.mylesdk.Constant;
import com.getmyle.mylesdk.MyleService1;
import com.getmyle.mylesdk.TapManager;
import com.getmyle.mylesdk.TapManager1;

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
            mEdBTLOC, mEdUUID, mEdVERSION,
            mEdBattery;


    private TapManager.ParameterReadListener listener = new TapManager.ParameterReadListener() {
        @Override
        public void onReadIntValue(final String param, final int value) {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (param.equals(Constant.DEVICE_PARAM_RECLN)) {
                        mEdRECLN.setText("" + value);
                    } else if (param.equals(Constant.DEVICE_PARAM_PAUSELEVEL)) {
                        mEdPAUSELEVEL.setText("" + value);
                    } else if (param.equals(Constant.DEVICE_PARAM_PAUSELEN)) {
                        mEdPAUSELEN.setText("" + value);
                    } else if (param.equals(Constant.DEVICE_PARAM_ACCELERSENS)) {
                        mEdACCELERSENS.setText("" + value);
                    } else if (param.equals(Constant.DEVICE_PARAM_BTLOC)) {
                        mEdBTLOC.setText("" + value);
                    }  else if (param.equals(Constant.DEVICE_PARAM_MIC)) {
                        mEdMIC.setText("" + value);
                    }
                }
            });
        }
        @Override
        public void onReadStringValue(final String param, final String value) {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (param.equals(Constant.DEVICE_PARAM_VERSION)) {
                        mEdVERSION.setText(value);
                    } else if (param.equals(Constant.DEVICE_PARAM_UUID)) {
                        mEdUUID.setText(value);
                    }
                }
            });
        }
        @Override
        public void onReadBatteryLevel(final int value) {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mEdBattery.setText("" + value);
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_parameter);

        getActionBar().setTitle("Parameter");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mEdRECLN = (EditText) findViewById(R.id.ed_recln);
        mEdPAUSELEVEL = (EditText) findViewById(R.id.ed_pause_level);
        mEdPAUSELEN = (EditText) findViewById(R.id.ed_pause_len);
        mEdACCELERSENS = (EditText) findViewById(R.id.ed_acceler_sens);
        mEdMIC = (EditText) findViewById(R.id.ed_mic);
        mEdPASSWORD = (EditText) findViewById(R.id.ed_password);
        mEdBTLOC = (EditText) findViewById(R.id.ed_btloc);
        mEdUUID = (EditText) findViewById(R.id.ed_uuid);
        mEdVERSION = (EditText) findViewById(R.id.ed_version);
        mEdBattery = (EditText) findViewById(R.id.ed_battery);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TapManager.getInstance().addParameterReadListener(listener);
        readAll();
    }

    @Override
    protected void onStop() {
        super.onStop();
        TapManager.getInstance().removeParameterReadListener(listener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void readAll() {
        TapManager.getInstance().sendReadRECLN();
        TapManager.getInstance().sendReadPAUSELEVEL();
        TapManager.getInstance().sendReadPAUSELEN();
        TapManager.getInstance().sendReadACCELERSENS();
        TapManager.getInstance().sendReadMIC();
        TapManager.getInstance().sendReadBTLOC();
        TapManager.getInstance().sendReadVERSION();
        TapManager.getInstance().sendReadUUID();
        TapManager.getInstance().sendReadBatteryLevel();

        // Set password
        String password = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(AppConstants.PREF_PASSWORD, AppConstants.DEFAULT_PASSWORD);
        mEdPASSWORD.setText(password);
    }

    public void clickReadAll(View v) throws InterruptedException {
        readAll();

        //Toast.makeText(this, "Read complete", Toast.LENGTH_LONG).show();
    }

    public void clickReadBatteryLevel(View v) throws InterruptedException {

        // Read Battery level
        //mTapManager.sendReadBATTERY_LEVEL();
    }

    public void clickEnableBatNotification(View v) throws InterruptedException {

        // Read Battery level
        //mTapManager.sendEnableBatteryNotification();
    }

    public void clickWriteAll(View v) throws InterruptedException {
        // Write RECLN
//        mTapManager.sendWriteRECLN(mEdRECLN.getText().toString());
//
//        // Write PAUSE_LEVEL
//        mTapManager.sendWritePAUSELEVEL(mEdPAUSELEVEL.getText().toString());
//
//        // Write PAUSE_LEN
//        mTapManager.sendWritePAUSELEN(mEdPAUSELEN.getText().toString());
//
//        // Write ACCELER_SENS
//        mTapManager.sendWriteACCELERSENS(mEdACCELERSENS.getText().toString());
//
//        // Write MIC
//        mTapManager.sendWriteMIC(mEdMIC.getText().toString());
//
//        // Write BTLOC
//        mTapManager.sendWriteBTLOC(mEdBTLOC.getText().toString());
//
//        // Write PASSWORD
//        mTapManager.sendWritePASSWORD(mEdPASSWORD.getText().toString());

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(AppConstants.PREF_PASSWORD, mEdPASSWORD.getText().toString())
                .apply();

        Toast.makeText(this, "Write complete", Toast.LENGTH_LONG).show();
    }

}
