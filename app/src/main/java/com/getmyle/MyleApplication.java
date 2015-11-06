package com.getmyle;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.getmyle.mylesdk.TapManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;


public class MyleApplication extends Application {

    // TapManager log is stored in logObservable to be easily accessed
    // and notified about changes from other app activities
    public static LogObservable logObservable = new LogObservable();

    private static SimpleDateFormat dt = new SimpleDateFormat("mm:ss.SS");

    private static TapManager.TraceListener traceListener = new TapManager.TraceListener() {
        @Override
        public void onTrace(String msg) {
            logObservable.append(msg);
        }
    };

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
            return;
        }

        // Given that:
        // 1. We've just called TapManager.setup(), so MyleBleService is not bound to TapManager yet
        // 2. TapManager methods are waiting until MyleBleService is bound to it.
        // 3. MyleBleService is being bound in main thread and current method is on the same thread as well
        // we cannot call TapManager methods synchronously here, because deadlock would happen:
        // we would be waiting here for service to bound, but service is not going to bound
        // because it has to happen on the same thread.
        // Just to workaroud this specific case we better call TapManager method in another thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                TapManager.getInstance().addTraceListener(traceListener);
            }
        }).start();
    }


    public static class LogObservable extends Observable {
        private StringBuilder value = new StringBuilder();

        public void append(String msg) {
            this.value.insert(0, dt.format(new Date()) + ": " + msg + "\n");
            this.setChanged();
            this.notifyObservers(value.toString());
        }

        public void clear() {
            this.value = new StringBuilder();
            this.setChanged();
            this.notifyObservers(value.toString());
        }
    }
}
