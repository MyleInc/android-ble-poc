package com.getmyle;

import android.app.Activity;
import android.os.Bundle;
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

public class ParameterActivity extends Activity implements
        MyleService1.ParameterListener {

    private EditText mEdRECLN, mEdPAUSELEVEL,
            mEdPAUSELEN, mEdACCELERSENS,
            mEdMIC, mEdPASSWORD,
            mEdBTLOC, mEdUUID, mEdVERSION;

    private TapManager1 mTapManager;

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

        mTapManager = new TapManager1(this);
        //mTapManager.setTapManagerListener(this);

        //mTapManager.connectToService();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //mTapManager.destroy();
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

    public void clickReadAll(View v) throws InterruptedException {
        // Read RECLN
        mTapManager.sendReadRECLN();

        // Read PAUSE_LEVEL
        mTapManager.sendReadPAUSELEVEL();

        // Read PAUSE_LEN
        mTapManager.sendReadPAUSELEN();

        // Read ACCELER_SENS
        mTapManager.sendReadACCELERSENS();

        // Read MIC
        mTapManager.sendReadMIC();

        // Read BTLOC
        mTapManager.sendReadBTLOC();

        // Read Version
        mTapManager.sendReadVERSION();

        // Read password
        // Set uuid
        mEdUUID.setText(Constant.SERVICE_UUID.toString());

        // Set password
        String password = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(AppConstants.PREF_PASSWORD, AppConstants.DEFAULT_PASSWORD);
        mEdPASSWORD.setText(password);

        Toast.makeText(this, "Read complete", Toast.LENGTH_LONG).show();
    }

    public void clickReadBatteryLevel(View v) throws InterruptedException {

        // Read Battery level
        mTapManager.sendReadBATTERY_LEVEL();
    }

    public void clickEnableBatNotification(View v) throws InterruptedException {

        // Read Battery level
        mTapManager.sendEnableBatteryNotification();
    }

    public void clickWriteAll(View v) throws InterruptedException {
        // Write RECLN
        mTapManager.sendWriteRECLN(mEdRECLN.getText().toString());

        // Write PAUSE_LEVEL
        mTapManager.sendWritePAUSELEVEL(mEdPAUSELEVEL.getText().toString());

        // Write PAUSE_LEN
        mTapManager.sendWritePAUSELEN(mEdPAUSELEN.getText().toString());

        // Write ACCELER_SENS
        mTapManager.sendWriteACCELERSENS(mEdACCELERSENS.getText().toString());

        // Write MIC
        mTapManager.sendWriteMIC(mEdMIC.getText().toString());

        // Write BTLOC
        mTapManager.sendWriteBTLOC(mEdBTLOC.getText().toString());

        // Write PASSWORD
        mTapManager.sendWritePASSWORD(mEdPASSWORD.getText().toString());

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(AppConstants.PREF_PASSWORD, mEdPASSWORD.getText().toString())
                .apply();

        Toast.makeText(this, "Write complete", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReceive(String name, int intValue, String strValue) {
        if (name.equals(Constant.DEVICE_PARAM_RECLN)) {
            mEdRECLN.setText(strValue);
        } else if (name.equals(Constant.DEVICE_PARAM_PAUSELEVEL)) {
            mEdPAUSELEVEL.setText(strValue);
        } else if (name.equals(Constant.DEVICE_PARAM_PAUSELEN)) {
            mEdPAUSELEN.setText(strValue);
        } else if (name.equals(Constant.DEVICE_PARAM_ACCELERSENS)) {
            mEdACCELERSENS.setText(strValue);
        } else if (name.equals(Constant.DEVICE_PARAM_BTLOC)) {
            mEdBTLOC.setText(strValue);
        } else if (name.equals(Constant.DEVICE_PARAM_VERSION)) {
            mEdVERSION.setText(strValue);
        } else if (name.equals(Constant.DEVICE_PARAM_MIC)) {
            mEdMIC.setText(strValue);
        }
    }

    public void clickloct(View v) {
    }

}
