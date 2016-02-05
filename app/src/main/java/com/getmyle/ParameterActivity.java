package com.getmyle;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.getmyle.mylesdk.Constants;
import com.getmyle.mylesdk.TapManager;

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


    private TapManager.CharacteristicValueListener listener = new TapManager.CharacteristicValueListener() {
        @Override
        public void onIntValue(final String param, final int value) {
            if (param.equals(Constants.DEVICE_PARAM_RECLN)) {
                mEdRECLN.setText("" + value);
            } else if (param.equals(Constants.DEVICE_PARAM_PAUSELEVEL)) {
                mEdPAUSELEVEL.setText("" + value);
            } else if (param.equals(Constants.DEVICE_PARAM_PAUSELEN)) {
                mEdPAUSELEN.setText("" + value);
            } else if (param.equals(Constants.DEVICE_PARAM_ACCELERSENS)) {
                mEdACCELERSENS.setText("" + value);
            } else if (param.equals(Constants.DEVICE_PARAM_BTLOC)) {
                mEdBTLOC.setText("" + value);
            }  else if (param.equals(Constants.DEVICE_PARAM_MIC)) {
                mEdMIC.setText("" + value);
            }
        }
        @Override
        public void onStringValue(final String param, final String value) {
            if (param.equals(Constants.DEVICE_PARAM_VERSION)) {
                mEdVERSION.setText(value);
            } else if (param.equals(Constants.DEVICE_PARAM_UUID)) {
                mEdUUID.setText(value);
            }
        }
        @Override
        public void onBatteryLevel(final int value) {
            mEdBattery.setText("" + value);
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
        TapManager.getInstance().addCharacteristicValueListener(listener);
        readAll();
    }

    @Override
    protected void onStop() {
        super.onStop();
        TapManager.getInstance().removeCharacteristicValueListener(listener);
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
        TapManager.getInstance().readRECLN();
        TapManager.getInstance().readPAUSELEVEL();
        TapManager.getInstance().readPAUSELEN();
        TapManager.getInstance().readACCELERSENS();
        TapManager.getInstance().readMIC();
        TapManager.getInstance().readBTLOC();
        TapManager.getInstance().readVERSION();
        TapManager.getInstance().readUUID();
        TapManager.getInstance().readBatteryLevel();
    }


    public void clickWriteAll(View v) throws InterruptedException {
        TapManager.getInstance().writeRECLN(Integer.parseInt(mEdRECLN.getText().toString()));
        TapManager.getInstance().writePAUSELEVEL(Integer.parseInt(mEdPAUSELEVEL.getText().toString()));
        TapManager.getInstance().writePAUSELEN(Integer.parseInt(mEdPAUSELEN.getText().toString()));
        TapManager.getInstance().writeACCELERSENS(Integer.parseInt(mEdACCELERSENS.getText().toString()));
        TapManager.getInstance().writeMIC(Integer.parseInt(mEdMIC.getText().toString()));
        TapManager.getInstance().writeBTLOC(Integer.parseInt(mEdBTLOC.getText().toString()));

        if (!mEdPASSWORD.getText().toString().isEmpty()) {
            TapManager.getInstance().writePASSWORD(mEdPASSWORD.getText().toString());

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(AppConstants.PREF_PASSWORD, mEdPASSWORD.getText().toString())
                    .apply();
        }

        Toast.makeText(this, "Write complete", Toast.LENGTH_LONG).show();
    }

}
