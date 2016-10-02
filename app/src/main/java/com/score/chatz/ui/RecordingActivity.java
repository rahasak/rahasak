package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.AudioRecorder;
import com.score.chatz.utils.ImageUtils;
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

    private CountDownTimer cancelTimer;
    private CountDownTimer recordTimer;
    private AudioRecorder audioRecorder;

    private Senz thisSenz;
    private SenzorsDbSource dbSource;

    private static final int TIME_TO_SERVE_REQUEST = 30000;
    private static final int START_TIME = 7;

    private float dX, dY, startX, startY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        Intent intent = getIntent();
        thisSenz = intent.getParcelableExtra("Senz");
        dbSource = new SenzorsDbSource(this);
        audioRecorder = new AudioRecorder();

        setupUi();
        setupDontBtn();
        setupSwipeBtns();
        startBtnAnimations();
        startVibrations();
        setupHandlesForSwipeBtnContainers();
        setupPhotoRequestTitle();
        setupWakeLock();
        startTimerToEndRequest();
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
    protected void onDestroy() {
        super.onDestroy();

        stopVibrations();
        clearFlags();
    }

    private void setupUi() {
        this.mTimerTextView = (TextView) this.findViewById(R.id.timer);
        this.mTimerTextView.setText(START_TIME + "");
        this.moving_layout = findViewById(R.id.moving_layout);
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
        ((TextView) findViewById(R.id.photo_request_user_name)).setText(" @" + thisSenz.getSender().getUsername());
        ((TextView) findViewById(R.id.photo_request_user_name)).setTypeface(typeface, Typeface.NORMAL);
    }

    private void setupUserImage() {
        String userImage = new SenzorsDbSource(this).getImageFromDB(thisSenz.getSender().getUsername());
        if (userImage != null)
            ((ImageView) findViewById(R.id.user_profile_image)).setImageBitmap(new ImageUtils().decodeBitmap(userImage));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startBtnRectRelativeToScreen = new Rect(startBtn.getLeft(), startBtn.getTop(), startBtn.getRight(), startBtn.getBottom());
            cancelBtnRectRelativeToScreen = new Rect(cancelBtn.getLeft(), cancelBtn.getTop(), cancelBtn.getRight(), cancelBtn.getBottom());
        }
    }

    private void startTimerToEndRequest() {
        cancelTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
            @Override
            public void onFinish() {
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
                    stopVibrations();
                    cancelTimerToServe();
                    startRecording();
                    moving_layout.setVisibility(View.INVISIBLE);
                    doneBtn.setVisibility(View.VISIBLE);
                } else if (cancelBtnRectRelativeToScreen.contains((int) (event.getRawX()), (int) (event.getRawY()))) {
                    // Inside cancel button region
                    stopVibrations();
                    cancelTimerToServe();
                    sendBusySenz();
                    this.finish();
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
        audioRecorder.startRecording();
        recordTimer = new CountDownTimer(START_TIME * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                updateQuickCountTimer((int) millisUntilFinished / 1000);
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
        audioRecorder.stopRecording();
        if (audioRecorder.getRecording() != null) {
            Secret secret = getSoundSecret(thisSenz.getSender(), thisSenz.getReceiver(), Base64.encodeToString(audioRecorder.getRecording().toByteArray(), 0));
            Long _timeStamp = System.currentTimeMillis();
            secret.setTimeStamp(_timeStamp);
            String uid = SenzUtils.getUniqueRandomNumber();
            secret.setId(uid);
            dbSource.createSecret(secret);
            sendSound(secret, this, uid);

            this.finish();
        }
    }

    private Secret getSoundSecret(User sender, User receiver, String sound) {
        Secret secret = new Secret(sound, "SOUND", SecretsUtil.getTheUser(sender, receiver, getApplicationContext()), false);
        secret.setReceiver(sender);
        return secret;
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
        Senz _senz = new Senz(id, signature, senzType, thisSenz.getReceiver(), thisSenz.getSender(), senzAttributes);
        send(_senz);
    }

    private void sendSound(final Secret secret, final Context context, final String uid) {
        // compose senzes
        Senz startSenz = getStartSoundSharingSenz(secret, context, uid);
        ArrayList<Senz> micSenzList = getSoundStreamingSenz(secret, context, uid);
        Senz stopSenz = getStopSoundSharingSenz(secret, context, uid);

        ArrayList<Senz> senzList = new ArrayList<>();
        senzList.add(startSenz);
        senzList.addAll(micSenzList);
        senzList.add(stopSenz);

        sendInOrder(senzList);
    }

    private ArrayList<Senz> getSoundStreamingSenz(Secret secret, Context context, String uid) {
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
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("mic", aSound.trim());
            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, SecretsUtil.getCurrentUser(context), secret.getUser(), senzAttributes);
            senzList.add(_senz);
        }
        return senzList;
    }

    private Senz getStartSoundSharingSenz(Secret secret, Context context, String uid) {
        //senz is the original senz
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("mic", "on");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;
        return new Senz(id, signature, senzType, SecretsUtil.getCurrentUser(context), secret.getReceiver(), senzAttributes);
    }

    private Senz getStopSoundSharingSenz(Secret secret, Context context, String uid) {
        // create senz attributes
        //senz is the original senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("mic", "off");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;
        return new Senz(id, signature, senzType, SecretsUtil.getCurrentUser(context), secret.getReceiver(), senzAttributes);
    }

    private String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }
}







