package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.CameraUtils;
import com.score.chatz.utils.ImageUtils;
import com.score.chatz.utils.SecretsUtil;
import com.score.chatz.utils.SenzUtils;
import com.score.chatz.utils.VibrationUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;
import com.skyfishjy.library.RippleBackground;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class PhotoActivity extends BaseActivity implements View.OnTouchListener {
    protected static final String TAG = PhotoActivity.class.getName();

    // camera related variables
    private android.hardware.Camera mCamera;
    private CameraPreview mCameraPreview;
    private boolean isPhotoTaken;
    private boolean isPhotoCancelled;

    // UI elements
    private View callingUserInfo;
    private View buttonControls;
    private TextView quickCountdownText;
    private Rect startBtnRectRelativeToScreen;
    private Rect cancelBtnRectRelativeToScreen;
    private CircularImageView cancelBtn;
    private ImageView startBtn;
    private RippleBackground goRipple;

    private Senz originalSenz;
    private PowerManager.WakeLock wakeLock;
    private CountDownTimer cancelTimer;

    private static final int TIME_TO_SERVE_REQUEST = 30000;
    private static final int TIME_TO_QUICK_PHOTO = 3000;
    private static final int IMAGE_SIZE = 110;

    private float dX, dY, startX, startY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        originalSenz = getIntent().getParcelableExtra("Senz");

        //init camera
        initCameraPreview();
        setupCameraSurface();

        // init activity
        setupUi();
        setupSwipeBtns();
        startBtnAnimations();
        startVibrations();
        setupHandlesForSwipeBtnContainers();
        setupPhotoRequestTitle();
        setupWakeLock();
        getSupportActionBar().hide();
        startTimerToEndRequest();
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

    private void setupWakeLock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

//        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
//        wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
//        wakeLock.acquire();
    }

    private void clearFlags() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setupCameraSurface() {
        FrameLayout preview = (FrameLayout) findViewById(R.id.photo);
        preview.addView(mCameraPreview);
    }

    private void setupUi() {
        quickCountdownText = (TextView) findViewById(R.id.quick_count_down);
        quickCountdownText.setVisibility(View.INVISIBLE);
        callingUserInfo = findViewById(R.id.sender_info);
        buttonControls = findViewById(R.id.moving_layout);
    }

    private void hideUiControls() {
        callingUserInfo.setVisibility(View.INVISIBLE);
        buttonControls.setVisibility(View.INVISIBLE);
    }

    private void setupPhotoRequestTitle() {
        ((TextView) findViewById(R.id.photo_request)).setText(getResources().getString(R.string.photo_request) + " @" + originalSenz.getSender().getUsername());
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
        clearFlags();
        stopVibrations();
        releaseCamera();
//        if (wakeLock != null)
//            wakeLock.release();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            Log.d(TAG, "Stopping preview in SurfaceDestroyed().");
            mCamera.release();
        }
    }

    private void setupSwipeBtns() {
        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        startBtn = (ImageView) findViewById(R.id.start);
    }

    private void startBtnAnimations() {
        Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        goRipple = (RippleBackground) findViewById(R.id.go_ripple);
        goRipple.startRippleAnimation();
        goRipple.startAnimation(anim);
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

    @Override
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
                    if (!isPhotoTaken) {
                        cancelTimerToServe();
                        startQuickCountdownToPhoto();
                        isPhotoTaken = true;
                    }
                } else if (cancelBtnRectRelativeToScreen.contains((int) (event.getRawX()), (int) (event.getRawY()))) {
                    // Inside cancel button region
                    if (!isPhotoCancelled) {
                        isPhotoCancelled = true;
                        cancelTimerToServe();
                        sendBusySenz();
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

    private void startQuickCountdownToPhoto() {
        quickCountdownText.setVisibility(View.VISIBLE);
        hideUiControls();

        new CountDownTimer(TIME_TO_QUICK_PHOTO, 1000) {
            public void onTick(long millisUntilFinished) {
                updateQuicCountTimer((int) millisUntilFinished / 1000);
            }

            public void onFinish() {
                takePhoto();
            }
        }.start();
    }

    /**
     * Helper method to access the camera returns null if
     * it cannot get the camera or does not exist
     *
     * @return
     */
    private void initCameraPreview() {
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            mCameraPreview = new CameraPreview(this, mCamera);
        } catch (Exception e) {
            // cannot get camera or does not exist
            Log.e(TAG, "No font cam");
        }
    }

    private void takePhoto() {
        quickCountdownText.setVisibility(View.INVISIBLE);
        CameraUtils.shootSound(this);
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                //byte[] resizedImage = CameraUtils.getCompressedImage(resizeBitmapByteArray(bytes, 90), IMAGE_SIZE);
                //byte[] resizedImage = getResizedBitmapLessThan500KB(bytes, 300);
                byte[] resizedImage = new ImageUtils().compressImage(bytes);

                sendPhotoSenz(resizedImage, originalSenz, PhotoActivity.this);
                Intent i = new Intent(PhotoActivity.this, PhotoFullScreenActivity.class);
                i.putExtra("IMAGE", Base64.encodeToString(resizedImage, 0));
                i.putExtra("QUICK_PREVIEW", "true");
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }

    private void updateQuicCountTimer(final int count) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                quickCountdownText.setText(count + "");
            }
        });
    }

    private void startTimerToEndRequest() {
        cancelTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
            @Override
            public void onFinish() {
                sendBusySenz();
                PhotoActivity.this.finish();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Time in count down -" + millisUntilFinished);
            }
        }.start();
    }

    private void sendBusySenz() {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("status", "801");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, originalSenz.getReceiver(), originalSenz.getSender(), senzAttributes);
        send(_senz);
    }

    private void cancelTimerToServe() {
        if (cancelTimer != null)
            cancelTimer.cancel();
    }

    private byte[] resizeBitmapByteArray(byte[] data, int deg) {
        Bitmap decodedBitmap = CameraUtils.decodeBase64(Base64.encodeToString(data, 0));
        // set max width ~ 600px
        Bitmap rotatedBitmap = CameraUtils.getAdjustedImage(decodedBitmap, deg, 1000);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    /**
     * Util methods
     */
    private void sendPhotoSenz(final byte[] image, final Senz senz, final Context context) {
        // compose senz
        String uid = SenzUtils.getUniqueRandomNumber();

        // stream on senz
        // stream content
        // stream off senz
        Senz startStreamSenz = getStartStreamSenz(senz);
        ArrayList<Senz> photoSenzList = getPhotoStreamSenz(senz, image, context, uid);
        Senz stopStreamSenz = getStopStreamSenz(senz);

        // populate list
        ArrayList<Senz> senzList = new ArrayList<>();
        senzList.add(startStreamSenz);
        senzList.addAll(photoSenzList);
        senzList.add(stopStreamSenz);

        sendInOrder(senzList);
    }

    /**
     * Decompose image stream in to multiple data/stream senz's
     *
     * @param senz    original senz
     * @param image   image content
     * @param context app context
     * @param uid     unique id
     * @return list of decomposed senz's
     */
    private ArrayList<Senz> getPhotoStreamSenz(Senz senz, byte[] image, Context context, String uid) {
        String imageString = Base64.encodeToString(image, Base64.DEFAULT);

        // save photo to db before sending if its a cam
        if (senz.getAttributes().containsKey("cam")) {
            //Secret newSecret = new Secret(imageString, "IMAGE", senz.getReceiver(), true);
            User user = SecretsUtil.getTheUser(senz.getSender(), senz.getReceiver(), context);
            Secret newSecret = new Secret(imageString, "IMAGE", user, SecretsUtil.isThisTheUsersSecret(user, senz.getReceiver()));
            Long timeStamp = System.currentTimeMillis();
            newSecret.setTimeStamp(timeStamp);
            newSecret.setID(uid);
            new SenzorsDbSource(context).createSecret(newSecret);
        }

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] packets = split(imageString, 1024);

        for (String packet : packets) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.STREAM;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            if (senz.getAttributes().containsKey("cam")) {
                senzAttributes.put("cam", packet.trim());
            }

            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
            senzList.add(_senz);
        }

        return senzList;
    }

    /**
     * Create start stream senz
     *
     * @param senz original senz
     * @return senz
     */
    private Senz getStartStreamSenz(Senz senz) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("cam", "on");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        return new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
    }

    /**
     * Create stop stream senz
     *
     * @param senz original senz
     * @return senz
     */
    private Senz getStopStreamSenz(Senz senz) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("cam", "off");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        return new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
    }

    private String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }

    public static byte[] getResizedBitmapLessThan500KB(byte[] data, int maxSize) {
        Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        Bitmap reduced_bitmap = Bitmap.createScaledBitmap(image, width, height, true);

        Matrix matrix = new Matrix();
        matrix.postRotate(-90);

        Bitmap rBitMap = Bitmap.createBitmap(reduced_bitmap, 0, 0, reduced_bitmap.getWidth(), reduced_bitmap.getHeight(), matrix, true);


        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rBitMap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        System.out.println(byteArray.length / 1024 + "size------");

        return byteArray;
    }
}