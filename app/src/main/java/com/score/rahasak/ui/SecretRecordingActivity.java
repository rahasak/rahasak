package com.score.rahasak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.rahasak.R;
import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;
import com.score.rahasak.pojo.Secret;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.AudioRecorder;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.SenzParser;
import com.score.rahasak.utils.SenzUtils;
import com.score.rahasak.utils.VibrationUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SecretRecordingActivity extends AppCompatActivity {

    private static final String TAG = SecretRecordingActivity.class.getName();

    private View moving_layout;
    private Button doneBtn;
    private CircularImageView cancelBtn;
    private ImageView startBtn;
    private TextView briefIntroTextView;
    private TextView countDownTextView;

    private boolean isRecordingStarted;
    private boolean isRecordingOver;

    private String username;
    private SecretUser secretUser;
    private SenzorsDbSource dbSource;
    private AudioRecorder audioRecorder;

    private static final int TIME_TO_SERVE_REQUEST = 15000;
    private static final int START_TIME = 7;

    protected Typeface typeface;

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

    private CountDownTimer requestTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
        @Override
        public void onFinish() {
            sendBusySenz();
            //saveMissedCall();
            SecretRecordingActivity.this.finish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.i(TAG, "Time in count down -" + millisUntilFinished);

        }
    };

    private CountDownTimer recordTimer = new CountDownTimer(START_TIME * 1000, 1000) {
        public void onTick(long millisUntilFinished) {
            updateQuickCountTimer((int) millisUntilFinished / 1000);
        }

        public void onFinish() {
            stopRecording();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        Intent intent = getIntent();
        dbSource = new SenzorsDbSource(this);
        audioRecorder = new AudioRecorder();

        username = intent.getStringExtra("USER");
        secretUser = dbSource.getSecretUser(username);

        setUpFonts();
        setupUi();
        setupDontBtn();
        //startBtnAnimations();
        startVibrations();
        //setupHandlesForSwipeBtnContainers();
        setupPhotoRequestTitle();
        setupWakeLock();
        startTimerToEndRequest();
        setupUserImage();
        startMicIfMissedCall();
    }

    private void setUpFonts() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");
    }

    private void startMicIfMissedCall() {
        if (getIntent().hasExtra("MISSED_AUDIO_CALL")) {
            stopVibrations();
            cancelTimerToServe();
            startRecording();
            moving_layout.setVisibility(View.INVISIBLE);
            doneBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // user on call
        SenzApplication.setOnCall(true);

        Log.d(TAG, "Bind to senz service");
        bindToService();
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
    }

    protected void bindToService() {
        Intent intent = new Intent("com.score.rahasak.remote.SenzService");
        intent.setPackage(this.getPackageName());
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");
        countDownTextView = (TextView) this.findViewById(R.id.time_countdown);
        countDownTextView.setText(START_TIME + "");
        briefIntroTextView = (TextView) findViewById(R.id.share_secret_brief);
        moving_layout = findViewById(R.id.moving_layout);


        countDownTextView.setTypeface(typeface, Typeface.BOLD);
        briefIntroTextView.setTypeface(typeface, Typeface.NORMAL);

        countDownTextView.setVisibility(View.INVISIBLE);
        briefIntroTextView.setVisibility(View.INVISIBLE);

        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecordingOver) {
                    isRecordingOver = true;

                    stopVibrations();
                    cancelTimerToServe();
                    sendBusySenz();
                    //saveMissedCall();
                    SecretRecordingActivity.this.finish();
                }
            }
        });
        startBtn = (ImageView) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecordingStarted) {
                    isRecordingStarted = true;

                    stopVibrations();
                    cancelTimerToServe();
                    startRecording();

                    moving_layout.setVisibility(View.INVISIBLE);
                    doneBtn.setVisibility(View.VISIBLE);
                    countDownTextView.setVisibility(View.VISIBLE);
                    briefIntroTextView.setVisibility(View.VISIBLE);
                }
            }
        });
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

    private void setupDontBtn() {
        doneBtn = (Button) findViewById(R.id.done_btn);
        doneBtn.setVisibility(View.INVISIBLE);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordTimer.cancel();
                stopRecording();
            }
        });
    }

    private void setupPhotoRequestTitle() {
        ((TextView) findViewById(R.id.photo_request_header)).setTypeface(typeface, Typeface.NORMAL);
        ((TextView) findViewById(R.id.photo_request_user_name)).setText(" @" + secretUser.getUsername());
        ((TextView) findViewById(R.id.photo_request_user_name)).setTypeface(typeface, Typeface.NORMAL);
    }

    private void setupUserImage() {
        if (secretUser.getImage() != null)
            ((ImageView) findViewById(R.id.user_profile_image)).setImageBitmap(new ImageUtils().decodeBitmap(secretUser.getImage()));
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

    private void startRecording() {
        audioRecorder.startRecording();
        recordTimer.start();
    }

    private void updateQuickCountTimer(final int count) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countDownTextView.setText(count + "");
            }
        });
    }

    private void stopRecording() {
        Log.d(TAG, "Stop recording ---");
        // stops the recording activity
        audioRecorder.stopRecording();
        if (audioRecorder.getRecording() != null) {
            String sound = Base64.encodeToString(audioRecorder.getRecording().toByteArray(), 0);
            Secret secret = new Secret(sound, BlobType.SOUND, secretUser, false);
            Long timeStamp = System.currentTimeMillis() / 1000;
            secret.setTimeStamp(timeStamp);
            String uid = SenzUtils.getUid(this, timeStamp.toString());
            secret.setId(uid);
            secret.setDeliveryState(DeliveryState.PENDING);
            dbSource.createSecret(secret);
            sendSound(secret, uid, timeStamp);

            this.finish();
        }
    }

    private void sendBusySenz() {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        Long timestamp = System.currentTimeMillis() / 1000;
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("status", "MIC_BUSY");
        senzAttributes.put("uid", SenzUtils.getUid(this, timestamp.toString()));

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
        send(_senz);
    }

    private void sendSound(final Secret secret, final String uid, final Long timestamp) {
        // compose senzes
        Senz startSenz = getStartSoundSharingSenz(uid, timestamp);
        ArrayList<Senz> micSenzList = getSoundStreamingSenz(secret, uid, timestamp);
        Senz stopSenz = getStopSoundSharingSenz(uid, timestamp);

        ArrayList<Senz> senzList = new ArrayList<>();
        senzList.add(startSenz);
        senzList.addAll(micSenzList);
        senzList.add(stopSenz);

        sendInOrder(senzList);
    }

    private ArrayList<Senz> getSoundStreamingSenz(Secret secret, String uid, Long timestamp) {
        String soundString = secret.getBlob();

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] sound = split(soundString, 1024);
        for (String aSound : sound) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.STREAM;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", timestamp.toString());
            senzAttributes.put("mic", aSound.trim());
            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
            senzList.add(_senz);
        }
        return senzList;
    }

    private Senz getStartSoundSharingSenz(String uid, Long timestamp) {
        //senz is the original senz
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("mic", "on");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;
        return new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
    }

    private Senz getStopSoundSharingSenz(String uid, Long timestamp) {
        // create senz attributes
        //senz is the original senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("mic", "off");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;
        return new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
    }

    private String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }

    public void send(Senz senz) {
        if (NetworkUtil.isAvailableNetwork(this)) {
            try {
                if (isServiceBound) {
                    senzService.send(senz);
                } else {
                    Log.d(TAG, "send senz " + SenzParser.getSenzPayload(senz));
                    ActivityUtils.showCustomToast("Failed to connected to service.", this);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            ActivityUtils.showCustomToast(getResources().getString(R.string.no_internet), this);
        }
    }

    public void sendInOrder(List<Senz> senzList) {
        if (NetworkUtil.isAvailableNetwork(this)) {
            try {
                if (isServiceBound) {
                    senzService.sendInOrder(senzList);
                } else {
                    Log.d(TAG, "send senzlist " + senzList.size());
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







