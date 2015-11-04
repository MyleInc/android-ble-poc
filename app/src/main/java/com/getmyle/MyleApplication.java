package com.getmyle;

import android.app.Application;

import com.getmyle.mylesdk.TapManager;


public class MyleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            TapManager.setup(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
