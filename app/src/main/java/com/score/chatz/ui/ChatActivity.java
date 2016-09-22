package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.utils.PreferenceUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;


public class ChatActivity extends BaseActivity {

    private static final String TAG = ChatActivity.class.getName();

    private User receiver;
    private User sender;
    private ChatFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        startService();

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
        startService();

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
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_open_profile:
                openProfileView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // bind to senz service
        this.registerReceiver(senzMessageReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
        this.registerReceiver(senzPacketTimeoutReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.PACKET_TIMEOUT));

    }


    @Override
    public void onStop() {
        super.onStop();
        this.unregisterReceiver(senzMessageReceiver);
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

    private BroadcastReceiver senzPacketTimeoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            mainFragment.onPacketTimeout(senz);
        }
    };

    private void openProfileView(){
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("SENDER", sender.getUsername());
        startActivity(intent);
    }
}
