package com.score.rahasak.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.async.StreamRecorder;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.interfaces.IStreamListener;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

public class SecretCallActivity extends AppCompatActivity implements IStreamListener {

    private static final String TAG = SecretCallActivity.class.getName();

    private Typeface typeface;

    private View loadingView;
    private TextView playingText;
    private TextView usernameText;
    private TextView callingText;
    private ImageView waitingIcon;


    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

    private SecretUser secretUser;

    private StreamRecorder streamRecorder;

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

    // senz message
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");

            // extract senz
            if (intent.hasExtra("SENZ")) {
                Senz senz = intent.getExtras().getParcelable("SENZ");
                switch (senz.getSenzType()) {
                    case DATA:
                        onSenzReceived(senz);
                        break;
                    case STREAM:
                        onStreamReceived(senz);
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
        setContentView(R.layout.activity_audio_full_screen);

        initUi();
        changeStatusBarColor();
        initUser();
        startWaiting();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // user on call
        SenzApplication.setOnCall(true);

        Intent intent = new Intent("com.score.rahasak.remote.SenzService");
        intent.setPackage(this.getPackageName());
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // user not on call
        SenzApplication.setOnCall(false);

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
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sendEndStream();
        endCall();
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        waitingIcon = (ImageView) findViewById(R.id.selfie_image);
        loadingView = findViewById(R.id.mic_loading_view);

        playingText = (TextView) findViewById(R.id.mic_playingText);
        usernameText = (TextView) findViewById(R.id.mic_username);
        callingText = (TextView) findViewById(R.id.mic_calling_text);

        usernameText.setTypeface(typeface, Typeface.BOLD);
        callingText.setTypeface(typeface, Typeface.NORMAL);
        playingText.setTypeface(typeface, Typeface.NORMAL);
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
    }

    private void initUser() {
        secretUser = getIntent().getParcelableExtra("USER");
        usernameText.setText("@" + secretUser.getUsername());
    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            if (senz.getAttributes().get("status").equalsIgnoreCase("BUSY")) {
                // user busy
                displayInformationMessageDialog("BUSY", "User busy at this moment");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("offline")) {
                // offline
                displayInformationMessageDialog("OFFLINE", "User not available at this moment");
            }
        }
    }

    private void onStreamReceived(Senz senz) {
        if (SenzUtils.isStreamOn(senz)) {
            // on call
            sendStartSenz();
            startCall();
        } else if (SenzUtils.isStreamOff(senz)) {
            // end call
            sendEndStream();
            endCall();
        }
    }

    private void startWaiting() {
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();
    }

    private void startCall() {
        loadingView.setVisibility(View.INVISIBLE);
        playingText.setVisibility(View.VISIBLE);

        // start recorder
        if (streamRecorder == null)
            streamRecorder = new StreamRecorder(this, this);
        streamRecorder.start();
    }

    private void endCall() {
        if (streamRecorder != null)
            streamRecorder.stop();
    }

    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.information_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText(title);
        messageTextView.setText(Html.fromHtml(message));

        // set custom font
        messageHeaderTextView.setTypeface(typeface, Typeface.BOLD);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                SecretCallActivity.this.finish();
            }
        });

        dialog.show();
    }

    private void sendStartSenz() {
        Long timeStamp = System.currentTimeMillis() / 1000;
        String uid = SenzUtils.getUid(this, timeStamp.toString());

        //senz is the original senz
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timeStamp.toString());
        senzAttributes.put("mic", "on");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
        send(senz);
    }

    private void sendEndStream() {
        Long timeStamp = System.currentTimeMillis() / 1000;
        String uid = SenzUtils.getUid(this, timeStamp.toString());

        // create senz attributes
        //senz is the original senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timeStamp.toString());
        senzAttributes.put("mic", "off");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
        send(senz);
    }

    @Override
    public void onStream(String stream) {
        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        Long timeStamp = System.currentTimeMillis() / 1000;
        String uid = SenzUtils.getUid(this, timeStamp.toString());

        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timeStamp.toString());
        senzAttributes.put("mic", stream);
        senzAttributes.put("uid", uid);

        Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
        send(senz);
    }

    private void send(Senz senz) {
        if (NetworkUtil.isAvailableNetwork(this)) {
            try {
                if (isServiceBound) {
                    senzService.send(senz);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            ActivityUtils.showCustomToast(getResources().getString(R.string.no_internet), this);
        }
    }
}