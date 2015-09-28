package com.getmyle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;

/*
 * Show password.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class PasswordSettingActivity extends Activity {

    private EditText mEdPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_passwordsetting);
        getActionBar().setTitle(getResources().getString(R.string.passwordsetting_ac_actionbar_title));

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

        startActivity(new Intent(this, ScanActivity.class));
    }
}
