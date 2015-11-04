package com.getmyle.mylesdk;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Created by mikalai on 2015-11-04.
 */
public class MyleBleService extends Service {

    private static final String CLASS_TAG = MyleBleService.class.getSimpleName();

    private final IBinder binder = new LocalBinder();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private HashMap<String, BluetoothDevice> availableTaps = new HashMap<>(10);
    private HashMap<String, String> availableTapNames = new HashMap<>(10);


    @Override
    public void onCreate() {
        super.onCreate();

        this.bluetoothManager = (BluetoothManager) this.getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = this.bluetoothManager.getAdapter();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        BluetoothLeScanner scanner = this.bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        // NOTE: for some reason filtering by UUID doesn't work
        // that's why we display all BTLE devices we found
        // more details: http://stackoverflow.com/a/18899320/444630
        // So we manually check for service UUID below in onScanResult
//        ScanFilter filter = new ScanFilter.Builder()
//                .setServiceUuid(ParcelUuid.fromString(Constant.SERVICE_UUID))
//                .build();
//        filters.add(filter);

        final Service me = this;

        // scan for TAPs
        scanner.startScan(filters, settings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // filtering by service UUID above doesn't work
                // so we use trick from http://stackoverflow.com/a/24539704/444630
                // to extract service UUID
                if (!Utils.parseUuidsByAdvertisedData(result.getScanRecord().getBytes()).contains(Constant.SERVICE_UUID)) { return; }

                String address = result.getDevice().getAddress();

                if (availableTaps.containsKey(address)) { return; }

                String name = Utils.getNameByScanRecord(result.getScanRecord().getBytes());

                Log.i(CLASS_TAG, "Found tap " + name + " " + address);

                availableTaps.put(address, result.getDevice());
                availableTapNames.put(address, name);

                Intent intent = new Intent(Constant.TAP_NOTIFICATION_SCAN);
                LocalBroadcastManager.getInstance(me.getApplication()).sendBroadcast(intent);
            }
        });

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // this is needed to bind TapManager to this service,
        // so TapManager has access to current instance
        return binder;
    }


    public class LocalBinder extends Binder {
        public MyleBleService getServerInstance() {
            return MyleBleService.this;
        }
    }


    public Collection<BluetoothDevice> getAvailableTaps() {
        return this.availableTaps.values();
    }

    public String getTapName(String address) {
        return this.availableTapNames.get(address);
    }



}
