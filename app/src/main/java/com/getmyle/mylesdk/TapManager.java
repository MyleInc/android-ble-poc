package com.getmyle.mylesdk;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.Collection;

/**
 * Created by mikalai on 2015-11-04.
 */
public class TapManager implements ServiceConnection {

    private static volatile TapManager instance;

    Application app;
    MyleBleService service;


    TapManager(Application app) {
        this.app = app;
    }


    public static void setup(Application app) throws Exception {
        if (instance != null) { throw new Exception("TapManager is already instanciated"); }

        synchronized (TapManager.class) {
            if (instance != null) { throw new Exception("TapManager is already instanciated"); }

            instance = new TapManager(app);
        }

        // start Myle service
        Intent intent = new Intent(app, MyleBleService.class);

        // service is going to run infinitely in background
        app.startService(intent);

        // connect to service to get it's instance
        app.bindService(intent, instance, 0);
    }


    public static TapManager getInstance() {
        return instance;
    }



    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MyleBleService.LocalBinder localBinder = (MyleBleService.LocalBinder) binder;
        this.service = localBinder.getServerInstance();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
    }



    public Collection<BluetoothDevice> getAvailableTaps() {
        return this.service.getAvailableTaps();
    }


    public String getTapName(String address) {
        return this.service.getTapName(address);
    }

}
