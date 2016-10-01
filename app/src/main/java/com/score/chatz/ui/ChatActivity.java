package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.ImageUtils;
import com.score.chatz.utils.LimitedList;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = ChatActivity.class.getName();

    // UI components
    private EditText txtSecret;
    private TextView btnSend;
    private ImageButton btnLocation;
    private ImageButton btnPhoto;
    private ImageButton btnMic;
    private Toolbar toolbar;
    private ImageView btnBack;
    private ImageView btnUserSetting;

    // secret list
    private ListView listView;
    private ChatListAdapter secretAdapter;

    private User thisUser;
    private LimitedList<Secret> secretList;

    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

    // service connection
    protected ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);
            isServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Disconnected from senz service");
            senzService = null;
            isServiceBound = false;
        }
    };

    // senz received
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            switch (senz.getSenzType()) {
                case DATA:
                    onDataReceived(senz);
                    break;
                case STREAM:
                    onSenzStreamReceived(senz);
                    break;
                case SHARE:
                    onSenzShareReceived(senz);
                    break;
                default:
                    break;
            }
        }
    };

    // senz timeout
    private BroadcastReceiver senzTimeoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Time out for senz");

            Senz senz = intent.getExtras().getParcelable("SENZ");
            onSenzTimeoutReceived(senz);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initUi();
        initUser();
        setupToolbar();
        setupActionBar();
        initSecretList();
        updatePermissions();
        setupUserImage();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Bind to senz service");
        bindToService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // unbind from service
        if (isServiceBound) {
            Log.d(TAG, "Unbind to senz service");
            unbindService(senzServiceConnection);

            isServiceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // bind to senz service
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));
        registerReceiver(senzTimeoutReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.TIMEOUT));

        // init list again
        initSecretList();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
        unregisterReceiver(senzTimeoutReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // keep only last message
        new SenzorsDbSource(this).deleteAllSecretsExceptLast(thisUser.getUsername());
    }

    protected void bindToService() {
        Intent intent = new Intent("com.score.chatz.remote.SenzService");
        intent.setPackage(this.getPackageName());
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onClick(View v) {
        if (v == btnSend) {
            onClickSend();
        } else if (v == btnLocation) {
            onClickLocation();
        } else if (v == btnPhoto) {
            onClickPhoto();
        } else if (v == btnMic) {
            onClickMic();
        } else if (v == btnBack) {
            finish();
        } else if (v == btnUserSetting) {
            navigateToProfile();
        }
    }

    private void initUi() {
        // init
        txtSecret = (EditText) findViewById(R.id.text_message);
        txtSecret.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf"), Typeface.NORMAL);
        txtSecret.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        btnSend = (TextView) findViewById(R.id.sendBtn);
        btnSend.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf"), Typeface.BOLD);

        btnLocation = (ImageButton) findViewById(R.id.getLocBtn);
        btnPhoto = (ImageButton) findViewById(R.id.getCamBtn);
        btnMic = (ImageButton) findViewById(R.id.getMicBtn);

        // set click listeners
        btnSend.setOnClickListener(this);
        btnLocation.setOnClickListener(this);
        btnPhoto.setOnClickListener(this);
        btnMic.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.messages_list_view);
        listView.setDivider(null);
        listView.setDividerHeight(0);
    }

    private void initUser() {
        String username = getIntent().getStringExtra("SENDER");
        thisUser = new User("", username);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.chat_activity_header, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        ((TextView) findViewById(R.id.user_name)).setText("@" + thisUser.getUsername());

        btnBack = (ImageView) getSupportActionBar().getCustomView().findViewById(R.id.back_btn);
        btnUserSetting = (ImageView) getSupportActionBar().getCustomView().findViewById(R.id.user_profile_image);
        btnBack.setOnClickListener(this);
        btnUserSetting.setOnClickListener(this);
    }

    private void setupUserImage(){
        String userImage = new SenzorsDbSource(this).getImageFromDB(this.thisUser.getUsername());
        if(userImage != null)
        btnUserSetting.setImageBitmap(new ImageUtils().decodeBitmap(userImage));
    }

    private void initSecretList() {
        secretList = new LimitedList<>(7);
        for (Secret secret : new SenzorsDbSource(this).getSecretz(thisUser)) {
            secretList.add(secret);
        }

        secretAdapter = new ChatListAdapter(this, secretList);
        listView.setAdapter(secretAdapter);
    }

    private void navigateToProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("SENDER", thisUser.getUsername());
        startActivity(intent);
    }

    private void navigateToPhotoWait() {
        Intent intent = new Intent(this, PhotoFullScreenActivity.class);
        startActivity(intent);
    }

    private void navigateMicWait() {
        Intent intent = new Intent(this, AudioFullScreenActivity.class);
        startActivity(intent);
    }

    private void onClickSend() {
        String secretMsg = txtSecret.getText().toString().trim();
        if (!secretMsg.isEmpty()) {
            // clear text
            txtSecret.setText("");

            // create secret
            Secret secret = new Secret(secretMsg, "TEXT", thisUser, false);
            secret.setReceiver(thisUser);
            secret.setTimeStamp(System.currentTimeMillis());
            secret.setID(SenzUtils.getUniqueRandomNumber());

            // send secret
            // save secret
            sendSecret(secret);
            saveSecretInDb(secret);

            // update list view
            secretList.add(secret);
            secretAdapter.notifyDataSetChanged();
        }
    }

    private void onClickLocation() {
        // Go to locations waiting page
        navigateToLocationView();

        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("lat", "");
        senzAttributes.put("lon", "");
        senzAttributes.put("uid", SenzUtils.getUniqueRandomNumber());

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.GET;
        Senz senz = new Senz(id, signature, senzType, null, thisUser, senzAttributes);

        send(senz);
    }

    private void onClickPhoto() {
        navigateToPhotoWait();

        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("cam", "");
        senzAttributes.put("uid", SenzUtils.getUniqueRandomNumber());

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.GET;
        Senz senz = new Senz(id, signature, senzType, null, thisUser, senzAttributes);

        send(senz);
    }

    private void onClickMic() {
        navigateMicWait();

        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("mic", "");
        senzAttributes.put("uid", SenzUtils.getUniqueRandomNumber());

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.GET;
        Senz senz = new Senz(id, signature, senzType, null, thisUser, senzAttributes);

        send(senz);
    }

    private void sendSecret(Secret secret) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("msg", URLEncoder.encode(secret.getBlob(), "UTF-8"));
            String timeStamp = ((Long) (System.currentTimeMillis() / 1000)).toString();
            senzAttributes.put("time", timeStamp);
            senzAttributes.put("uid", secret.getID());

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;
            Senz senz = new Senz(id, signature, senzType, null, thisUser, senzAttributes);

            send(senz);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void saveSecretInDb(Secret secret) {
        SenzorsDbSource dbSource = new SenzorsDbSource(this);
        dbSource.createSecret(secret);
    }

    public void onDataReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            // status message
            String msg = senz.getAttributes().get("status");
            if (msg != null && msg.equalsIgnoreCase("700")) {
                onSenzStatusReceived(senz);
            } else if (msg != null && msg.equalsIgnoreCase("901")) {
                Toast.makeText(this, "User busy", Toast.LENGTH_LONG).show();
            }
        } else if (senz.getAttributes().containsKey("msg")) {
            // chat message
            onNewSenzReceived(senz);
        } else if (senz.getAttributes().containsKey("lat")) {
            // location received
        }
    }

    private void onSenzStreamReceived(Senz senz) {
        onNewSenzReceived(senz);
    }

    private void onSenzShareReceived(Senz senz) {
        updatePermissions();
    }

    public void onSenzTimeoutReceived(Senz senz) {
        String uid = senz.getAttributes().get("uid");
        new SenzorsDbSource(this).markSecretDeliveryFailed(uid);

        // update failed message in list
        for (Secret secret : secretList) {
            if (secret.getID().equalsIgnoreCase(uid)) {
                secret.setDeliveryFailed(true);
                secretAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onSenzStatusReceived(Senz senz) {
        // update senz in db
        String uid = senz.getAttributes().get("uid");
        new SenzorsDbSource(this).markSecretDelievered(uid);

        for (Secret secret : secretList) {
            if (secret.getID().equalsIgnoreCase(uid)) {
                secret.setIsDelivered(true);
                secretAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onNewSenzReceived(Senz senz) {
        try {
            Secret secret;
            if (senz.getAttributes().containsKey("msg")) {
                String msg = URLDecoder.decode(senz.getAttributes().get("msg"), "UTF-8");
                secret = new Secret(msg, "TEXT", thisUser, true);
            } else if (senz.getAttributes().containsKey("mic")) {
                secret = new Secret(senz.getAttributes().get("mic"), "SOUND", thisUser, true);
            } else {
                secret = new Secret(senz.getAttributes().get("cam"), "PHOTO", thisUser, true);
            }
            secret.setReceiver(senz.getReceiver());
            secret.setTimeStamp(System.currentTimeMillis());
            secret.setID(senz.getAttributes().get("uid"));

            secretList.add(secret);
            secretAdapter.notifyDataSetChanged();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    private void updatePermissions() {
        UserPermission userPerm = new SenzorsDbSource(this).getUserPermission(thisUser);
        if (userPerm != null) {
            // location
            if (userPerm.getLocPerm()) {
                btnLocation.setImageResource(R.drawable.perm_locations_active);
                btnLocation.setEnabled(true);
            } else {
                btnLocation.setImageResource(R.drawable.perm_locations_deactive);
                btnLocation.setEnabled(false);
            }

            // camera
            if (userPerm.getCamPerm()) {
                btnPhoto.setImageResource(R.drawable.perm_camera_active);
                btnPhoto.setEnabled(true);
            } else {
                btnPhoto.setImageResource(R.drawable.perm_camera_deactive);
                btnPhoto.setEnabled(false);
            }

            // mic
            if (userPerm.getMicPerm()) {
                btnMic.setImageResource(R.drawable.perm_mic_active);
                btnMic.setEnabled(true);
            } else {
                btnMic.setImageResource(R.drawable.perm_mic_deactive);
                btnMic.setEnabled(false);
            }
        }
    }

    private void navigateToLocationView() {
        // start map activity
        Intent mapIntent = new Intent(this, SenzMapActivity.class);
        startActivity(mapIntent);
        overridePendingTransition(R.anim.right_in, R.anim.stay_in);
    }

    public void send(Senz senz) {
        if (NetworkUtil.isAvailableNetwork(this)) {
            try {
                if (isServiceBound) {
                    senzService.send(senz);
                } else {
                    ActivityUtils.showCustomToast("Failed to connected to service.", this);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            ActivityUtils.showCustomToast("No network connection available.", this);
        }
    }

}
