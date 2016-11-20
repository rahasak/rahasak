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
import com.score.chatz.application.SenzApplication;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.enums.BlobType;
import com.score.chatz.enums.DeliveryState;
import com.score.chatz.pojo.Permission;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.ImageUtils;
import com.score.chatz.utils.LimitedList;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.RSAUtils;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.net.URLEncoder;
import java.util.ArrayList;
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

    private SecretUser secretUser;
    private LimitedList<Secret> secretList;

    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

    private Typeface typeface;

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
            if (intent.hasExtra("SENZ")) {
                Senz senz = intent.getExtras().getParcelable("SENZ");
                switch (senz.getSenzType()) {
                    case DATA:
                        onDataReceived(senz);
                        break;
                    case STREAM:
                        onSenzStreamReceived(senz);
                        break;
                    case SHARE:
                        onShareRecived(senz);
                        break;
                    default:
                        break;
                }
            }
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initUser(intent);
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

        // on chat
        SenzApplication.setOnChat(true);
        SenzApplication.setUserOnChat(secretUser.getUsername());

        // bind to senz service
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));

        // update list
        updateSecretList();
    }

    @Override
    public void onPause() {
        super.onPause();

        // not on chat
        SenzApplication.setOnChat(false);
        SenzApplication.setUserOnChat(null);

        unregisterReceiver(senzReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // keep only last message
        new SenzorsDbSource(this).deleteAllSecretsExceptLast(secretUser.getUsername());
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
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        // init
        txtSecret = (EditText) findViewById(R.id.text_message);
        txtSecret.setTypeface(typeface, Typeface.NORMAL);
        txtSecret.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        btnSend = (TextView) findViewById(R.id.sendBtn);
        btnSend.setTypeface(typeface, Typeface.BOLD);

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
        String username = getIntent().getExtras().getString("SENDER");
        secretUser = new SenzorsDbSource(this).getSecretUser(username);
    }

    private void initUser(Intent intent) {
        String username = intent.getStringExtra("SENDER");
        secretUser = new SenzorsDbSource(this).getSecretUser(username);
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

        TextView header = ((TextView) findViewById(R.id.user_name));
        header.setTypeface(typeface, Typeface.BOLD);
        header.setText("@" + secretUser.getUsername());

        btnBack = (ImageView) getSupportActionBar().getCustomView().findViewById(R.id.back_btn);
        btnUserSetting = (ImageView) getSupportActionBar().getCustomView().findViewById(R.id.user_profile_image);
        btnBack.setOnClickListener(this);
        btnUserSetting.setOnClickListener(this);
    }

    private void setupUserImage() {
        if (secretUser.getImage() != null)
            btnUserSetting.setImageBitmap(new ImageUtils().decodeBitmap(secretUser.getImage()));
    }

    private void initSecretList() {
        ArrayList<Secret> tmpList = new SenzorsDbSource(this).getSecrets(secretUser);
        secretList = new LimitedList<>(tmpList.size());
        for (Secret secret : tmpList) {
            secretList.add(secret);
        }

        secretAdapter = new ChatListAdapter(this, secretList);
        listView.setAdapter(secretAdapter);
    }

    private void updateSecretList() {
        if (secretAdapter != null && secretList.size() > 0) {
            Secret lastSecret = secretList.getYongest();
            if (lastSecret != null) {
                ArrayList<Secret> tmpList = new SenzorsDbSource(this).getSecrets(secretUser, lastSecret.getTimeStamp());
                secretList.addAll(tmpList);
                secretAdapter.notifyDataSetChanged();
            }
        }
    }

    private void navigateToProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("SECRET_USER", secretUser);
        startActivity(intent);
    }

    private void navigateToPhotoWait() {
        Intent intent = new Intent(this, PhotoFullScreenActivity.class);
        intent.putExtra("SENDER", secretUser.getUsername());
        startActivity(intent);
    }

    private void navigateMicWait() {
        Intent intent = new Intent(this, AudioFullScreenActivity.class);
        intent.putExtra("SENDER", secretUser.getUsername());
        startActivity(intent);
    }

    private void onClickSend() {
        if (NetworkUtil.isAvailableNetwork(this)) {
            String secretMsg = txtSecret.getText().toString().trim();
            if (!secretMsg.isEmpty()) {
                // clear text
                txtSecret.setText("");

                // create secret
                Secret secret = new Secret(secretMsg, BlobType.TEXT, secretUser, false);
                Long timestamp = System.currentTimeMillis() / 1000;
                secret.setTimeStamp(timestamp);
                secret.setId(SenzUtils.getUid(this, timestamp.toString()));
                secret.setDeliveryState(DeliveryState.PENDING);

                // send secret
                // save secret
                sendSecret(secret);
                saveSecretInDb(secret);

                // update list view
                secretList.add(secret);
                secretAdapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
        }
    }

    private void onClickLocation() {
        if (NetworkUtil.isAvailableNetwork(this)) {
            ActivityUtils.showProgressDialog(this, "Please wait");

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();

            senzAttributes.put("lat", "");
            senzAttributes.put("lon", "");

            Long timestamp = System.currentTimeMillis() / 1000;
            senzAttributes.put("time", timestamp.toString());
            senzAttributes.put("uid", SenzUtils.getUid(this, timestamp.toString()));

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.GET;
            Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);

            send(senz);
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
        }
    }

    private void onClickPhoto() {
        if (NetworkUtil.isAvailableNetwork(this)) {
            navigateToPhotoWait();

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("cam", "");

            Long timestamp = System.currentTimeMillis() / 1000;
            senzAttributes.put("time", timestamp.toString());
            senzAttributes.put("uid", SenzUtils.getUid(this, timestamp.toString()));

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.GET;
            Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);

            send(senz);
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
        }
    }

    private void onClickMic() {
        if (NetworkUtil.isAvailableNetwork(this)) {
            navigateMicWait();

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("mic", "");

            Long timestamp = System.currentTimeMillis() / 1000;
            senzAttributes.put("time", timestamp.toString());
            senzAttributes.put("uid", SenzUtils.getUid(this, timestamp.toString()));

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.GET;
            Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);

            send(senz);
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
        }
    }

    private void sendSecret(Secret secret) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();

            // encrypt msg
            if (secretUser.getSessionKey() != null && !secretUser.getSessionKey().isEmpty()) {
                senzAttributes.put("$msg", RSAUtils.encrypt(RSAUtils.getSecretKey(secretUser.getSessionKey()), secret.getBlob()));
            } else {
                senzAttributes.put("msg", URLEncoder.encode(secret.getBlob(), "UTF-8"));
            }

            String timeStamp = ((Long) (System.currentTimeMillis() / 1000)).toString();
            senzAttributes.put("time", timeStamp);
            senzAttributes.put("uid", secret.getId());

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;
            Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);

            send(senz);
        } catch (Exception e) {
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
            if (msg != null && msg.equalsIgnoreCase("DELIVERED")) {
                // message delivered to user
                onSenzStatusReceived(senz);
            } else if (msg != null && msg.equalsIgnoreCase("NO_LOCATION")) {
                ActivityUtils.cancelProgressDialog();
                Toast.makeText(this, "No location available", Toast.LENGTH_LONG).show();
            }
        } else if (senz.getAttributes().containsKey("msg")) {
            // chat message
            onSenzMsgReceived(senz);
        } else if (senz.getAttributes().containsKey("lat")) {
            // location received
            ActivityUtils.cancelProgressDialog();
            onLocationReceived(senz);
        }
    }

    private void onSenzStreamReceived(Senz senz) {
        if (senz.getSender().getUsername().equalsIgnoreCase(secretUser.getUsername())) {
            Secret secret;
            if (senz.getAttributes().containsKey("cam")) {
                secret = new Secret(senz.getAttributes().get("cam"), BlobType.IMAGE, secretUser, true);
            } else {
                secret = new Secret(senz.getAttributes().get("mic"), BlobType.SOUND, secretUser, true);
            }
            secret.setTimeStamp(Long.parseLong(senz.getAttributes().get("time")));
            secret.setId(senz.getAttributes().get("uid"));
            secret.setDeliveryState(DeliveryState.PENDING);

            secretList.add(secret);
            secretAdapter.notifyDataSetChanged();
        }
    }

    private void onSenzStatusReceived(Senz senz) {
        // update senz in db
        String uid = senz.getAttributes().get("uid");
        new SenzorsDbSource(this).updateDeliveryStatus(DeliveryState.DELIVERED, uid);

        for (Secret secret : secretList) {
            if (secret.getId().equalsIgnoreCase(uid)) {
                secret.setDeliveryState(DeliveryState.DELIVERED);
                secretAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onSenzMsgReceived(Senz senz) {
        if (senz.getSender().getUsername().equalsIgnoreCase(secretUser.getUsername())) {
            if (senz.getAttributes().containsKey("msg")) {
                String msg = senz.getAttributes().get("msg");
                Secret secret = new Secret(msg, BlobType.TEXT, secretUser, true);
                secret.setTimeStamp(Long.parseLong(senz.getAttributes().get("time")));
                secret.setId(senz.getAttributes().get("uid"));
                secret.setDeliveryState(DeliveryState.PENDING);

                secretList.add(secret);
                secretAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onLocationReceived(Senz senz) {
        // start map activity
        Intent mapIntent = new Intent(this, SenzMapActivity.class);
        mapIntent.putExtra("SENZ", senz);
        startActivity(mapIntent);
    }

    private void onShareRecived(Senz senz) {
        secretUser = new SenzorsDbSource(this).getSecretUser(senz.getSender().getUsername());
        if (senz.getAttributes().containsKey("cam")) {
            updatePermissions();
        } else if (senz.getAttributes().containsKey("mic")) {
            updatePermissions();
        } else if (senz.getAttributes().containsKey("lat")) {
            updatePermissions();
        }
    }

    private void updatePermissions() {
        Permission permission = secretUser.getRecvPermission();
        if (permission != null) {
            // location
            if (permission.isLoc()) {
                btnLocation.setImageResource(R.drawable.perm_locations_active);
                btnLocation.setEnabled(true);
            } else {
                btnLocation.setImageResource(R.drawable.perm_locations_deactive);
                btnLocation.setEnabled(false);
            }

            // camera
            if (permission.isCam()) {
                btnPhoto.setImageResource(R.drawable.perm_camera_active);
                btnPhoto.setEnabled(true);
            } else {
                btnPhoto.setImageResource(R.drawable.perm_camera_deactive);
                btnPhoto.setEnabled(false);
            }

            // mic
            if (permission.isMic()) {
                btnMic.setImageResource(R.drawable.perm_mic_active);
                btnMic.setEnabled(true);
            } else {
                btnMic.setImageResource(R.drawable.perm_mic_deactive);
                btnMic.setEnabled(false);
            }
        }
    }

    private void send(Senz senz) {
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
            ActivityUtils.showCustomToast(getResources().getString(R.string.no_internet), this);
        }
    }

}
