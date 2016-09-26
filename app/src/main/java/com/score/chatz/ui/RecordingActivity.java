package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.handlers.SenzSoundHandler;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.AudioRecorder;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.SecretsUtil;
import com.score.chatz.utils.SenzUtils;
import com.score.chatz.utils.VibrationUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.HashMap;

public class RecordingActivity extends BaseActivity implements View.OnTouchListener {

    private static final String TAG = RecordingActivity.class.getName();

    private View moving_layout;
    private Button doneBtn;
    private TextView mTimerTextView;
    private Rect startBtnRectRelativeToScreen;
    private Rect cancelBtnRectRelativeToScreen;
    private CircularImageView cancelBtn;
    private ImageView startBtn;
    private RippleBackground goRipple;

    private int mStartTime = 10;
    private boolean isRecordingCancelled;
    private static boolean isActivityActive;

    private static final int TIME_TO_SERVE_REQUEST = 30000;

    private CountDownTimer cancelTimer;

    private User sender;
    private User receiver;

    private boolean isRecordingDone;
    private boolean hasRecordingStarted;

    SenzorsDbSource dbSource;

    AudioRecorder audioRecorder;

    private PowerManager.WakeLock wakeLock;

    private float dX, dY, startX, startY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        Intent intent = getIntent();
        try {
            receiver = PreferenceUtils.getUser(getApplicationContext());
        } catch (NoUserException e) {
            e.printStackTrace();
        }
        String senderString = intent.getStringExtra("SENDER");
        sender = new User("", senderString);

        if (isAcitivityActive()) {
            cancelTimerToServe();
            //SenzSoundHandler.getInstance().sendBusyNotification(new Senz(null, null, null, sender, receiver, null), this);
            sendBusySenz();
            this.finish();
        } else {
            isActivityActive = true;

            dbSource = new SenzorsDbSource(this);
            audioRecorder = new AudioRecorder();
            isRecordingDone = false;

            setupUi();
            setupDontBtn();
            setupSwipeBtns();
            startBtnAnimations();
            startVibrations();
            setupHandlesForSwipeBtnContainers();
            setupPhotoRequestTitle();
            setupWakeLock();
            startTimerToEndRequest();
        }
    }

    private void setupUi(){
        this.mTimerTextView = (TextView) this.findViewById(R.id.timer);
        this.mTimerTextView.setText(mStartTime + "");
        this.moving_layout = (View) findViewById(R.id.moving_layout);
    }

    private void setupWakeLock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
    }

    private void setupDontBtn() {
        doneBtn = (Button) findViewById(R.id.done_btn);
        doneBtn.setVisibility(View.INVISIBLE);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
    }

    private void setupPhotoRequestTitle() {
        ((TextView) findViewById(R.id.photo_request)).setText(getResources().getString(R.string.sound_request) + " @" + sender.getUsername());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startBtnRectRelativeToScreen = new Rect(startBtn.getLeft(), startBtn.getTop(), startBtn.getRight(), startBtn.getBottom());
            cancelBtnRectRelativeToScreen = new Rect(cancelBtn.getLeft(), cancelBtn.getTop(), cancelBtn.getRight(), cancelBtn.getBottom());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Release screen lock, so the phone can go back to sleep
        if (wakeLock != null)
            wakeLock.release();
        stopVibrations();
    }

    private void startTimerToEndRequest() {
        cancelTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
            @Override
            public void onFinish() {
                //SenzSoundHandler.getInstance().sendBusyNotification(new Senz(null, null, null, sender, receiver, null), RecordingActivity.this);
                sendBusySenz();
                RecordingActivity.this.finish();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Time in count down -" + millisUntilFinished);

            }
        }.start();
    }

    private void cancelTimerToServe() {
        if (cancelTimer != null)
            cancelTimer.cancel();
    }

    private void setupSwipeBtns() {
        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        startBtn = (ImageView) findViewById(R.id.start);
    }

    private void startVibrations() {
        VibrationUtils.startVibrationForPhoto(VibrationUtils.getVibratorPatterIncomingPhotoRequest(), this);
    }

    private void stopVibrations() {
        VibrationUtils.stopVibration(this);
    }

    private void setupHandlesForSwipeBtnContainers() {
        goRipple.setOnTouchListener(this);
    }

    private void startBtnAnimations() {
        Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        goRipple = (RippleBackground) findViewById(R.id.go_ripple);
        goRipple.startRippleAnimation();
        goRipple.startAnimation(anim);
    }

    public boolean onTouch(View v, MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        switch (event.getAction()) {
            case (MotionEvent.ACTION_DOWN):
                v.clearAnimation();
                startX = v.getX();
                startY = v.getY();
                dX = v.getX() - event.getRawX();
                dY = v.getY() - event.getRawY();

                break;

            case (MotionEvent.ACTION_MOVE):
                v.animate()
                        .x(event.getRawX() + dX)
                        .y(event.getRawY() + dY)
                        .setDuration(0)
                        .start();
                if (startBtnRectRelativeToScreen.contains((int) (event.getRawX()), (int) (event.getRawY()))) {
                    // Inside start button region
                    if (hasRecordingStarted == false) {
                        hasRecordingStarted = true;
                        stopVibrations();
                        cancelTimerToServe();
                        startRecording();
                        moving_layout.setVisibility(View.INVISIBLE);
                        doneBtn.setVisibility(View.VISIBLE);
                    }
                } else if (cancelBtnRectRelativeToScreen.contains((int) (event.getRawX()), (int) (event.getRawY()))) {
                    // Inside cancel button region
                    if (isRecordingCancelled == false) {
                        isRecordingCancelled = true;
                        cancelTimerToServe();
                        //SenzSoundHandler.getInstance().sendBusyNotification(new Senz(null, null, null, sender, receiver, null), this);
                        sendBusySenz();
                        stopVibrations();
                        stopRecording();
                    }
                }
                break;
            case (MotionEvent.ACTION_UP):
                v.animate()
                        .x(startX)
                        .y(startY)
                        .setDuration(0)
                        .start();
                Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
                v.startAnimation(anim);
                break;
        }
        return true;
    }

    private void startRecording() {
        audioRecorder.startRecording(getApplicationContext());
        new CountDownTimer(mStartTime * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                updateQuickCountTimer(--mStartTime);
            }

            public void onFinish() {
                stopRecording();
            }
        }.start();
    }

    private void updateQuickCountTimer(final int count) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimerTextView.setText(count + "");
            }
        });
    }

    private void stopRecording() {
        // stops the recording activity
        if (isRecordingDone == false) {
            isRecordingDone = true;
            audioRecorder.stopRecording();
            if (audioRecorder.getRecording() != null) {
                Secret secret = getSoundSecret(sender, receiver, Base64.encodeToString(audioRecorder.getRecording().toByteArray(), 0));
                Long _timeStamp = System.currentTimeMillis();
                secret.setTimeStamp(_timeStamp);
                String uid = SenzUtils.getUniqueRandomNumber().toString();
                secret.setID(uid);
                dbSource.createSecret(secret);
                sendSound(secret, this, uid);
            }
        }
        this.finish();
    }

    private Secret getSoundSecret(User sender, User receiver, String sound) {
        Secret secret = new Secret(sound, "SOUND", SecretsUtil.getTheUser(sender, receiver, getApplicationContext()), false);
        secret.setReceiver(sender);
        return secret;
    }

    @Override
    public void onStop() {
        super.onStop();
        isActivityActive = false;
    }

    public static boolean isAcitivityActive() {
        return isActivityActive;
    }

    private void sendBusySenz() {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("status", "901");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, receiver, sender, senzAttributes);
        send(_senz);
    }

    private void sendSound(final Secret secret, final Context context, final String uid) {
        // compose senzes
        Senz startSenz = getStartSoundSharingSenze(secret, context);
        ArrayList<Senz> photoSenzList = getSoundStreamingSenz(secret, context, uid);
        Senz stopSenz = getStopSoundSharingSenz(secret, context);

        ArrayList<Senz> senzList = new ArrayList<Senz>();
        senzList.add(startSenz);
        senzList.addAll(photoSenzList);
        senzList.add(stopSenz);

        sendInOrder(senzList);
    }

    private ArrayList<Senz> getSoundStreamingSenz(Secret secret, Context context, String uid) {
        String soundString = secret.getBlob();

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] sound = split(soundString, 1024);
        for (int i = 0; i < sound.length; i++) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.STREAM;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("mic", sound[i].trim());
            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, SecretsUtil.getCurrentUser(context), secret.getUser(), senzAttributes);
            senzList.add(_senz);
        }
        return senzList;
    }

    private Senz getStartSoundSharingSenze(Secret secret, Context context) {
        //senz is the original senz
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("mic", "on");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;
        Senz _senz = new Senz(id, signature, senzType, SecretsUtil.getCurrentUser(context), secret.getReceiver(), senzAttributes);
        return _senz;
    }

    private Senz getStopSoundSharingSenz(Secret secret, Context context) {
        // create senz attributes
        //senz is the original senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("mic", "off");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;
        Senz _senz = new Senz(id, signature, senzType, SecretsUtil.getCurrentUser(context), secret.getReceiver(), senzAttributes);
        return _senz;
    }

    private String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }
}







