package com.getmyle;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.getmyle.mylesdk.TapManager;


public class MyleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String address = pref.getString(AppConstants.PREF_ADDRESS, null);
        String password = pref.getString(AppConstants.PREF_PASSWORD, AppConstants.DEFAULT_PASSWORD);

        try {
            TapManager.setup(this, address, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
