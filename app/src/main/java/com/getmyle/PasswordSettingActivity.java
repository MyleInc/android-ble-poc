package com.getmyle;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.getmyle.mylesdk.TapManager;

/*
 * Show password.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class PasswordSettingActivity extends AppCompatActivity {

    public static final String INTENT_PARAM_UUID = "uuid";

    private EditText mEdPassword;
    private String mTapUUID;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_passwordsetting);
        getSupportActionBar().setTitle(getResources().getString(R.string.passwordsetting_ac_actionbar_title));

        mTapUUID = getIntent().getStringExtra(PasswordSettingActivity.INTENT_PARAM_UUID);

        mEdPassword = (EditText) findViewById(R.id.ed_password);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load old password
        String password = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(AppConstants.PREF_PASSWORD, AppConstants.DEFAULT_PASSWORD);
        mEdPassword.setText(password);
    }

    public void clickStart(View v) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(AppConstants.PREF_PASSWORD, mEdPassword.getText().toString())
                .apply();

        // connect to tap
        TapManager.getInstance().connectToTap(mTapUUID, mEdPassword.getText().toString());

        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
        finish();
    }
}
