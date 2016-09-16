package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.handlers.IntentProvider;
import com.score.chatz.utils.PreferenceUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;


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
        this.registerReceiver(senzMessageReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
        this.registerReceiver(userBusyNotifier, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.USER_BUSY));
        this.registerReceiver(senzPacketTimeoutReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.PACKET_TIMEOUT));

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
