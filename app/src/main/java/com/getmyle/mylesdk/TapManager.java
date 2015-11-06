package com.getmyle.mylesdk;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.provider.SyncStateContract;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

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


    public static void setup(Application app, String address, String password) throws Exception {
        if (instance != null) { throw new Exception("TapManager is already instanciated"); }

        synchronized (TapManager.class) {
            if (instance != null) { throw new Exception("TapManager is already instanciated"); }

            instance = new TapManager(app);
        }

        // start Myle service
        Intent intent = new Intent(app, MyleBleService.class);
        intent.putExtra(MyleBleService.INTENT_PARAM_INIT_ADDRESS, address);
        intent.putExtra(MyleBleService.INTENT_PARAM_INIT_PASSWORD, password);

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


    /**
     * Returns true if TapManager is bound to MyleBleService
     * @return
     */
    public boolean isReady() {
        return this.service != null;
    }


    public void startScan() {
        this.service.startScan();
    }


    public void stopScan() {
        this.service.stopScan();
    }


    public boolean isScanning() {
        return this.service.isScanning();
    }


    public Collection<BluetoothDevice> getAvailableTaps() {
        return this.service.getAvailableTaps();
    }


    public String getTapName(String address) {
        return this.service.getTapName(address);
    }


    public void connectToTap(String address, String password) {
        this.service.connectToTap(address, password);
    }


    public void disconnectFromCurrentTap() {
        this.service.disconnectFromCurrentTap();
    }


    public void forgetCurrentTap() {
        this.service.forgetCurrentTap();
    }


    public void sendReadRECLN() {
        this.service.sendReadRECLN();
    }

    public void sendReadBTLOC() {
        this.service.sendReadBTLOC();
    }

    public void sendReadPAUSELEVEL() {
        this.service.sendReadPAUSELEVEL();
    }

    public void sendReadPAUSELEN() {
        this.service.sendReadPAUSELEN();
    }

    public void sendReadACCELERSENS() {
        this.service.sendReadACCELERSENS();
    }

    public void sendReadMIC() {
        this.service.sendReadMIC();
    }

    public void sendReadVERSION() {
        this.service.sendReadVERSION();
    }

    public void sendReadUUID() {
        this.service.sendReadUUID();
    }

    public void sendReadBatteryLevel() {
        this.service.sendReadBatteryLevel();
    }


    public void addParameterReadListener(ParameterReadListener listener) {
        this.service.addParameterReadListener(listener);
    }


    public void removeParameterReadListener(ParameterReadListener listener){
        this.service.removeParameterReadListener(listener);
    }


    public static abstract class ParameterReadListener {
        public void onReadIntValue(String param, int value) {}
        public void onReadStringValue(String param, String value) {}
        public void onReadBatteryLevel(int value) {}
    }

}
