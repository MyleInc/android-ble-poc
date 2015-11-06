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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

/*
 * Display tasks log.
 * 
 * @author: Ideas&Solutions
 * @date: 03/30/2015
 */

public class LogActivity extends Activity implements Observer {

    private TextView tvLog;
    private Menu mMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // TextView
        tvLog = (TextView) findViewById(R.id.tv_log);
        if (savedInstanceState != null) {
            tvLog.setText(savedInstanceState.getString("LOG"));
        }

        // Setup actionbar
        getActionBar().setTitle(getResources().getString(R.string.log_ac_actionbar_title));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        MyleApplication.logObservable.addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        MyleApplication.logObservable.deleteObserver(this);
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
                MyleApplication.logObservable.clear();
                break;

            case R.id.action_parameter:
                startActivity(new Intent(this, ParameterActivity.class));
                break;

            case R.id.action_disconnect:
                TapManager.getInstance().disconnectFromCurrentTap();
                break;

            case R.id.action_forget_tap:
                TapManager.getInstance().forgetCurrentTap();
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

    @Override
    public void update(Observable observable, Object data) {
        if (observable != MyleApplication.logObservable) { return; }

        tvLog.setText(data.toString());
    }
}
