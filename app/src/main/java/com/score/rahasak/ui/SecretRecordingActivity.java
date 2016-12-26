package com.score.rahasak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.rahasak.R;
import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.async.StreamRecorder;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;
import com.score.rahasak.interfaces.IStreamListener;
import com.score.rahasak.pojo.Secret;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.SenzUtils;
import com.score.rahasak.utils.VibrationUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

public class SecretRecordingActivity extends AppCompatActivity implements IStreamListener {

    private static final String TAG = SecretRecordingActivity.class.getName();

    private RelativeLayout rootView;

    private FrameLayout callingUser;
    private TextView callingText;
    private TextView callingHeaderText;
    private TextView callingUsernameText;
    private ImageView waitingIcon;

    private CircularImageView cancelBtn;
    private CircularImageView startBtn;

    private SecretUser secretUser;

    private static final int TIME_TO_SERVE_REQUEST = 15000;

    protected Typeface typeface;

    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

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

    private CountDownTimer requestTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
        @Override
        public void onFinish() {
            sendBusySenz();
            SecretRecordingActivity.this.finish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        initUi();
        changeStatusBarColor();
        initUser();
        setupWakeLock();
        startVibrations();
        startTimerToEndRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // user on call
        SenzApplication.setOnCall(true);

        // bind to service
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
    protected void onDestroy() {
        super.onDestroy();

        stopVibrations();
        clearFlags();
        endCall();
        sendEndStream();
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        rootView = (RelativeLayout) findViewById(R.id.moving_layout);
        callingUser = (FrameLayout) findViewById(R.id.calling_user);
        callingHeaderText = (TextView) findViewById(R.id.mic_calling_text);
        callingUsernameText = (TextView) findViewById(R.id.mic_username);
        callingText = (TextView) findViewById(R.id.calling_text);

        callingHeaderText.setTypeface(typeface);
        callingUsernameText.setTypeface(typeface);
        callingText.setTypeface(typeface);

        waitingIcon = (ImageView) findViewById(R.id.selfie_image);
        startWaiting();

        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVibrations();
                cancelTimerToServe();
                sendBusySenz();
                endCall();
                SecretRecordingActivity.this.finish();
            }
        });

        startBtn = (CircularImageView) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVibrations();
                cancelTimerToServe();
                sendStartSenz();
                startCall();
            }
        });
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
    }

    private void startWaiting() {
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();
    }

    private void initUser() {
        String username = getIntent().getStringExtra("USER");
        secretUser = new SenzorsDbSource(this).getSecretUser(username);

        callingUsernameText.setText(" @" + secretUser.getUsername());
        if (secretUser.getImage() != null) {
            BitmapDrawable drawable = new BitmapDrawable(getResources(), new ImageUtils().decodeBitmap(secretUser.getImage()));
            callingUser.setBackground(drawable);
        } else {
            callingUser.setVisibility(View.GONE);
        }
    }

    private void setupWakeLock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void clearFlags() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void startTimerToEndRequest() {
        requestTimer.start();
    }

    private void cancelTimerToServe() {
        if (requestTimer != null)
            requestTimer.cancel();
    }

    private void startVibrations() {
        VibrationUtils.startVibrationForPhoto(VibrationUtils.getVibratorPatterIncomingPhotoRequest(), this);
    }

    private void stopVibrations() {
        VibrationUtils.stopVibration(this);
    }

    private void saveMissedCall() {
        Secret newSecret = new Secret("", BlobType.SOUND, secretUser, true);
        Long timeStamp = System.currentTimeMillis() / 1000;
        newSecret.setTimeStamp(timeStamp);
        newSecret.setId(SenzUtils.getUid(this, timeStamp.toString()));
        newSecret.setDeliveryState(DeliveryState.PENDING);
        new SenzorsDbSource(this).createSecret(newSecret);
    }

    private void startCall() {
        rootView.setVisibility(View.GONE);
        callingText.setVisibility(View.VISIBLE);

        if (streamRecorder == null)
            streamRecorder = new StreamRecorder(this, this);
        streamRecorder.start();
    }

    private void endCall() {
        if (streamRecorder != null)
            streamRecorder.stop();
    }

    private void sendBusySenz() {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        Long timestamp = System.currentTimeMillis() / 1000;
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("status", "BUSY");
        senzAttributes.put("uid", SenzUtils.getUid(this, timestamp.toString()));

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;

        Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
        send(senz);
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







