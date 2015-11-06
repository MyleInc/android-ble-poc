package com.getmyle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.getmyle.mylesdk.TapManager;

import java.util.Calendar;

/*
 * Display tasks log.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class LogActivity extends Activity {

    private TextView tvLog;
    private Menu mMenu;

    private TapManager.TraceListener traceListener = new TapManager.TraceListener() {
        @Override
        public void onTrace(String msg) {
            Calendar c = Calendar.getInstance();
            int seconds = c.get(Calendar.SECOND);
            int minute = c.get(Calendar.MINUTE);
            int hour = c.get(Calendar.HOUR_OF_DAY);

            tvLog.append(hour + ":" + minute + ":" + seconds + ": " + msg + "\n");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // TextView
        tvLog = (TextView) findViewById(R.id.tv_log);

        // Setup actionbar
        getActionBar().setTitle(getResources().getString(R.string.log_ac_actionbar_title));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Given that:
        // 1. Current activity is the first one, so it can happen that MyleBleService is not bound yet to TapManager
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TapManager.getInstance().removeTraceListener(traceListener);
    }



    // Clear log
    public void clickClearLog() {
        //tvLog.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {

            case R.id.action_clear:
                clickClearLog();
                break;

            case R.id.action_parameter:
                startActivity(new Intent(this, ParameterActivity.class));
                break;

            case R.id.action_disconnect:
                TapManager.getInstance().disconnectFromCurrentTap();
                break;

            case R.id.action_forget_tap:
                TapManager.getInstance().forgetCurrentTap();

                Toast.makeText(this, "Forgot current tap", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_get_num_rev_audio:
                //int num = mTapManager.getReceiveByteAudio();
                //Toast.makeText(this, num + " bytes", Toast.LENGTH_LONG).show();
                break;

            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(upIntent);
                finish();
                return true;
        }


        return super.onOptionsItemSelected(item);
    }
}
