package com.example.myle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/*
 * Show password.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class PasswordSettingActivity extends Activity{
    private static final String DEFAULT_PASSWORD = "1234abcd";
	private EditText mEdPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_passwordsetting);
		getActionBar().setTitle(getResources().getString(R.string.passwordsetting_ac_actionbar_title));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Load old password 
		SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
		String password = prefs.getString(Constant.SharedPrefencesKeyword.PASSWORD, DEFAULT_PASSWORD);
		mEdPassword = (EditText)findViewById(R.id.ed_password);
		mEdPassword.setText(password);
	}
 	
	public void clickStart(View v) {
    	SharedPreferences sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constant.SharedPrefencesKeyword.PASSWORD, mEdPassword.getText().toString());
        String address = sharedPref.getString(Constant.SharedPrefencesKeyword.PERIPHERAL_ADDRESS, null);
        editor.commit();
		
		if (address == null) {
			startActivity(new Intent(this, ScanActivity.class));
		} else {
			startActivity(new Intent(this, LogActivity.class));
		}
	}
}
