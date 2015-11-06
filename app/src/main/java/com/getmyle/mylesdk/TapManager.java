package com.getmyle.mylesdk;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * Created by mikalai on 2015-11-04.
 */
public class TapManager implements ServiceConnection {

    private static volatile TapManager instance;

    private Application app;
    private MyleBleService service;

    private final CountDownLatch latch = new CountDownLatch(1);


    TapManager(Application app) {
        this.app = app;
    }


    public static void setup(final Application app, final String address, final String password) throws Exception {
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
        this.latch.countDown();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
    }


    /**
     * NOTE: we wait for service to be available on current thread.
     * @return
     */
    private MyleBleService getService() {
        if (Looper.myLooper() == Looper.getMainLooper() && this.service == null) {
            Log.e(TapManager.class.getName(), "You have to call your method in another thread in order to be executed once MyleBleService is bound.");
        }
        try {
            this.latch.await();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        return this.service;
    }


    public void startScan() {
        getService().startScan();
    }


    public void stopScan() {
        getService().stopScan();
    }


    public boolean isScanning() {
        return getService().isScanning();
    }


    public Collection<BluetoothDevice> getAvailableTaps() {
        return getService().getAvailableTaps();
    }


    public String getTapName(String address) {
        return getService().getTapName(address);
    }


    public void connectToTap(String address, String password) {
        getService().connectToTap(address, password);
    }


    public void disconnectFromCurrentTap() {
        getService().disconnectFromCurrentTap();
    }


    public void forgetCurrentTap() {
        getService().forgetCurrentTap();
    }


    public void readRECLN() {
        getService().readRECLN();
    }

    public void readBTLOC() {
        getService().readBTLOC();
    }

    public void readPAUSELEVEL() {
        getService().readPAUSELEVEL();
    }

    public void readPAUSELEN() {
        getService().readPAUSELEN();
    }

    public void readACCELERSENS() {
        getService().readACCELERSENS();
    }

    public void readMIC() {
        getService().readMIC();
    }

    public void readVERSION() {
        getService().readVERSION();
    }

    public void readUUID() {
        getService().readUUID();
    }

    public void readBatteryLevel() {
        getService().readBatteryLevel();
    }


    public void addCharacteristicValueListener(CharacteristicValueListener listener) {
        getService().addCharacteristicValueListener(listener);
    }


    public void removeCharacteristicValueListener(CharacteristicValueListener listener){
        getService().removeCharacteristicValueListener(listener);
    }


    public void addTraceListener(TraceListener listener) {
        getService().addTraceListener(listener);
    }


    public void removeTraceListener(TraceListener listener){
        getService().removeTraceListener(listener);
    }


    public static abstract class CharacteristicValueListener {
        public void onIntValue(String param, int value) {}
        public void onStringValue(String param, String value) {}
        public void onBatteryLevel(int value) {}
    }


    public static abstract class TraceListener {
        public void onTrace(String msg) {}
    }

}
