package com.score.chatz.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.chatz.R;
import com.score.chatz.handlers.SenzPhotoHandler;
import com.score.chatz.pojo.SenzStream;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.CameraUtils;
import com.score.chatz.utils.VibrationUtils;
import com.score.senzc.pojos.Senz;
import com.skyfishjy.library.RippleBackground;

public class PhotoActivity extends AppCompatActivity implements View.OnTouchListener {
    protected static final String TAG = PhotoActivity.class.getName();

    private android.hardware.Camera mCamera;
    private CameraPreview mCameraPreview;
    private boolean isPhotoTaken;
    private boolean isPhotoCancelled;

    private View callingUserInfo;
    private View buttonControls;
    private TextView quickCountdownText;

    private PhotoActivity instnce;
    private Senz originalSenz;
    private PowerManager.WakeLock wakeLock;

    private Rect startBtnRectRelativeToScreen;
    private Rect cancelBtnRectRelativeToScreen;
    private CircularImageView cancelBtn;
    private ImageView startBtn;
    private RippleBackground goRipple;

    private CountDownTimer cancelTimer;

    private static final int TIME_TO_SERVE_REQUEST = 30000; // 30 seconds
    private static final int TIME_TO_QUICK_PHOTO = 3; // 3 seconds


    private float dX, dY, startX, startY;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_photo);
        instnce = this;
        mCameraPreview = CameraPreview.getSingleton(this, mCamera, (SenzStream.SENZ_STEAM_TYPE) getIntent().getExtras().get("StreamType"));

        try {
            originalSenz = (Senz) getIntent().getParcelableExtra("Senz");
        }catch (Exception ex){
            ex.printStackTrace();
        }

        if(mCameraPreview.isCameraBusy() == true) {
            SenzPhotoHandler.getInstance().sendBusyNotification(originalSenz, this);
            this.finish();
        }else{
            FrameLayout preview = (FrameLayout) findViewById(R.id.photo);
            try {
                preview.addView(mCameraPreview);
            }catch (IllegalStateException ex){
                ex.printStackTrace();
                ViewGroup parent = (ViewGroup) mCameraPreview.getParent();
                if (parent != null) {
                    parent.removeView(mCameraPreview);
                }
                preview.addView(mCameraPreview);
            }

            setupUiHandlers();
            setupSwipeBtns();
            startBtnAnimations();
            startVibrations();
            setupHandlesForSwipeBtnContainers();
            setupPhotoRequestTitle();

            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
            wakeLock.acquire();

            getSupportActionBar().hide();
            startTimerToEndRequest();
        }
    }

    private void setupUiHandlers(){
        quickCountdownText = (TextView) findViewById(R.id.quick_count_down);
        quickCountdownText.setVisibility(View.INVISIBLE);
        callingUserInfo = findViewById(R.id.sender_info);
        buttonControls = findViewById(R.id.moving_layout);
    }

    private void hideUiControls(){
        callingUserInfo.setVisibility(View.INVISIBLE);
        buttonControls.setVisibility(View.INVISIBLE);
    }

    private void setupPhotoRequestTitle(){
        ((TextView)findViewById(R.id.photo_request)).setText(getResources().getString(R.string.photo_request) + " @" + originalSenz.getSender().getUsername());
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus){
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
            if(wakeLock != null)
            wakeLock.release();
            stopVibrations();
            if (mCamera != null)
                mCamera.release();
    }

    private void setupSwipeBtns(){
        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        startBtn = (ImageView) findViewById(R.id.start);
    }

    private void startBtnAnimations(){
        Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        goRipple=(RippleBackground)findViewById(R.id.go_ripple);
        goRipple.startRippleAnimation();
        goRipple.startAnimation(anim);
    }

    private void startVibrations(){
        VibrationUtils.startVibrationForPhoto(VibrationUtils.getVibratorPatterIncomingPhotoRequest(), this);
    }

    private void stopVibrations(){
        VibrationUtils.stopVibration(this);
    }

    private void setupHandlesForSwipeBtnContainers() {
        goRipple.setOnTouchListener(this);
    }

    public boolean onTouch(View v, MotionEvent event) {
        int x = (int)event.getRawX();
        int y = (int)event.getRawY();
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
                if(startBtnRectRelativeToScreen.contains((int)(event.getRawX()), (int)(event.getRawY()))){
                    // Inside start button region
                    stopVibrations();
                    if(isPhotoTaken == false) {
                        cancelTimerToServe();
                        startQuickCountdownToPhoto(this);
                        isPhotoTaken = true;
                    }
                }else if(cancelBtnRectRelativeToScreen.contains((int)(event.getRawX()), (int)(event.getRawY()))){
                    // Inside cancel button region
                    if(isPhotoCancelled == false) {
                        isPhotoCancelled = true;
                        cancelTimerToServe();
                        SenzPhotoHandler.getInstance().sendBusyNotification(originalSenz, this);
                        stopVibrations();
                        this.finish();
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

    private void startQuickCountdownToPhoto(final Activity context){
        quickCountdownText.setVisibility(View.VISIBLE);
        hideUiControls();
        new Thread(new Runnable() {
            int mStartTime = TIME_TO_QUICK_PHOTO;
            @Override
            public void run() {
                while (mStartTime != 0) {
                    updateQuicCountTimer(mStartTime);
                    mStartTime--;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Log.e(TAG, "Ticker thread interrupted");
                    }
                }
                if(mStartTime == 0){
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            takePhoto();
                        }
                    });
                }
            }
        }).start();
    }

    private void takePhoto(){
        quickCountdownText.setVisibility(View.INVISIBLE);
        CameraUtils.shootSound(this);
        mCameraPreview.takePhotoManually(instnce, originalSenz);
    }

    private void updateQuicCountTimer(final int count) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                quickCountdownText.setText(count + "");
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCameraPreview.getHolder().removeCallback(mCameraPreview);
            mCamera.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get the Camera instance as the activity achieves full user focus
        if (mCamera == null) {
            //initializeCamera(); // Local method to handle camera initialization
            //openFrontFacingCameraGingerbread();
        }

    }

    private void startTimerToEndRequest(){
        final PhotoActivity _this = this;
        cancelTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST,TIME_TO_SERVE_REQUEST){
            @Override
            public void onFinish() {
                SenzPhotoHandler.getInstance().sendBusyNotification(originalSenz, _this);
                _this.finish();
            }
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Time in count down -" + millisUntilFinished);

            }
        }.start();
    }

    private void cancelTimerToServe(){
        if(cancelTimer != null)
        cancelTimer.cancel();
    }

    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


}