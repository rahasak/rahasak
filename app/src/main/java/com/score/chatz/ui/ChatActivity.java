package com.score.chatz.ui;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.handlers.AppIntentHandler;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.PreferenceUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


public class ChatActivity extends BaseActivity {

    //private Toolbar toolbar;
    private User receiver;
    private User sender;
    private ImageView backBtn;
    private static final String TAG = ChatActivity.class.getName();
    private ChatFragment mainFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        try {
            receiver = PreferenceUtils.getUser(getApplicationContext());
        } catch (NoUserException e) {
            e.printStackTrace();
        }
        String senderString = intent.getStringExtra("SENDER");
        sender = new User("", senderString);
        FragmentManager fm = getSupportFragmentManager();
        mainFragment = (ChatFragment) fm.findFragmentById(R.id.container_main);
        setupActionBar();

        if (mainFragment == null) {
            mainFragment = ChatFragment.newInstance(sender, receiver);
            fm.beginTransaction().add(R.id.container_main, mainFragment).commit();
        }


    }


    private void setupActionBar(){
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#636363")));
        getSupportActionBar().setTitle("@"+sender.getUsername());
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // bind to senz service
        this.registerReceiver(senzMessageReceiver, AppIntentHandler.getIntentFilter(AppIntentHandler.INTENT_TYPE.DATA_SENZ));
        this.registerReceiver(userBusyNotifier, AppIntentHandler.getIntentFilter(AppIntentHandler.INTENT_TYPE.USER_BUSY));
        this.registerReceiver(senzPacketTimeoutReceiver, AppIntentHandler.getIntentFilter(AppIntentHandler.INTENT_TYPE.PACKET_TIMEOUT));

    }


    @Override
    public void onStop() {
        super.onStop();
        this.unregisterReceiver(senzMessageReceiver);
        this.unregisterReceiver(userBusyNotifier);
        this.unregisterReceiver(senzPacketTimeoutReceiver);

    }


    private BroadcastReceiver senzMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");
            //handleMessage(intent);
            ((ChatFragment) getSupportFragmentManager().findFragmentById(R.id.container_main)).handleMessage(intent);
        }
    };

    private BroadcastReceiver userBusyNotifier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            displayInformationMessageDialog( getResources().getString(R.string.sorry),  senz.getSender().getUsername() + " " + getResources().getString(R.string.is_busy_now));
        }
    };

    private BroadcastReceiver senzPacketTimeoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            mainFragment.onPacketTimeout(senz);
        }
    };

}
