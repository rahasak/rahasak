package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;
import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.services.LocationAddressReceiver;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = ChatActivity.class.getName();

    // UI components
    private EditText txtSecret;
    private ImageButton btnSend;
    private ImageButton btnLocation;
    private ImageButton btnPhoto;
    private ImageButton btnMic;

    // secret list
    private ListView listView;
    private ChatFragmentListAdapter secretAdapter;

    private User thisUser;
    private List<Secret> secretList;

    // senz message
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");

            // extract senz
            Senz senz = intent.getExtras().getParcelable("SENZ");
            onSenzReceived(senz);
        }
    };

    // share senz
    private BroadcastReceiver senzShareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Share senz");

            Senz senz = intent.getExtras().getParcelable("SENZ");
            onSenzShareReceived(senz);
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
        initActionBar();
        initSecretList();
        updatePermissions();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateToHome();
                return true;
            case R.id.action_open_profile:
                navigateToProfile();
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
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
        registerReceiver(senzShareReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SHARE_SENZ));
        registerReceiver(senzTimeoutReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.PACKET_TIMEOUT));

        // init list again
        initSecretList();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(senzReceiver);
        unregisterReceiver(senzShareReceiver);
        unregisterReceiver(senzTimeoutReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // keep only last message
        deleteAllSecretsExceptTheLast();
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
        }
    }

    private void initUi() {
        // init
        txtSecret = (EditText) findViewById(R.id.text_message);
        btnSend = (ImageButton) findViewById(R.id.sendBtn);
        btnLocation = (ImageButton) findViewById(R.id.getLocBtn);
        btnPhoto = (ImageButton) findViewById(R.id.getCamBtn);
        btnMic = (ImageButton) findViewById(R.id.getMicBtn);

        // set click listeners
        btnSend.setOnClickListener(this);
        btnLocation.setOnClickListener(this);
        btnPhoto.setOnClickListener(this);
        btnMic.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.messages_list_view);
    }

    private void initUser() {
        String username = getIntent().getStringExtra("SENDER");
        thisUser = new User("", username);
    }

    private void initActionBar() {
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#636363")));
        getSupportActionBar().setTitle("@" + thisUser.getUsername());
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initSecretList() {
        secretList = new SenzorsDbSource(this).getSecretz(thisUser);
        secretAdapter = new ChatFragmentListAdapter(this, secretList);
        listView.setAdapter(secretAdapter);
    }

    private void navigateToProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("SENDER", thisUser.getUsername());
        startActivity(intent);
    }

    private void navigateToHome() {
        this.finish();
        Intent intent = new Intent(this, HomeActivity.class);
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

    public void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            // status message
            String msg = senz.getAttributes().get("status");
            if (msg != null && msg.equalsIgnoreCase("700")) {
                onSenzStatusReceived(senz);
            }else if (msg != null && msg.equalsIgnoreCase("901")) {
                displayInformationMessageDialog("Sorry", "User is busy.");
            }
        } else if (senz.getAttributes().containsKey("msg") || senz.getAttributes().containsKey("cam") || senz.getAttributes().containsKey("mic")) {
            // chat message
            onNewSenzReceived(senz);
        } else if (senz.getAttributes().containsKey("lat")) {
            onLocationReceived(senz);
        }
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
                playSound();
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

            playSound();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    private void onLocationReceived(Senz senz) {
        // location response received
        double lat = Double.parseDouble(senz.getAttributes().get("lat"));
        double lan = Double.parseDouble(senz.getAttributes().get("lon"));
        LatLng latLng = new LatLng(lat, lan);

        // start location address receiver
        new LocationAddressReceiver(this, latLng, senz.getSender()).execute("PARAM");
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

    private void playSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.message);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                mp.release();
            }

        });
        mediaPlayer.start();
    }

    private void deleteAllSecretsExceptTheLast() {
        // keep last secret
        if (secretList.size() > 1) {
            // remove last secret
            secretList.remove(secretList.get(secretList.size() - 1));
            for (Secret secret : secretList) {
                new SenzorsDbSource(this).deleteSecret(secret);
            }
        }
    }

    private void navigateToLocationView() {
        // TODO start map first
        // start map activity
        Intent mapIntent = new Intent(this, SenzMapActivity.class);
        //mapIntent.putExtra("extra", latLng);
        startActivity(mapIntent);
        overridePendingTransition(R.anim.right_in, R.anim.stay_in);
    }
}
