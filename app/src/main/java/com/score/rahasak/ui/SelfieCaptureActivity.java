package com.score.rahasak.ui;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
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
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.PhoneBookUtil;
import com.score.rahasak.utils.SenzUtils;
import com.score.rahasak.utils.VibrationUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;

public class SelfieCaptureActivity extends BaseActivity {
    protected static final String TAG = SelfieCaptureActivity.class.getName();

    // call details
    private FrameLayout callDeatilsLayout;
    private TextView selfiCallText;
    private TextView callerNameText;
    private TextView callerUsername;
    private ImageView selfieImage;

    // camera related variables
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private boolean isPhotoTaken;
    private boolean isPhotoCancelled;
    private boolean isFrontCam;

    // UI elements
    private View callingUserInfo;
    private View buttonControls;
    private TextView quickCountdownText;
    private CircularImageView cancelBtn;
    private ImageView startBtn;
    private ImageView rotateCamera;

    // selfie request user
    private SecretUser secretUser;

    private CountDownTimer cancelTimer;

    private static final int TIME_TO_SERVE_REQUEST = 15000;
    private static final int TIME_TO_QUICK_PHOTO = 3000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selfie_capture_activity_layout);

        //init camera
        initCameraPreview(Camera.CameraInfo.CAMERA_FACING_FRONT);
        initFlags();

        // init activity
        initUi();
        initUser();
        VibrationUtils.startVibrationForPhoto(VibrationUtils.getVibratorPatterIncomingPhotoRequest(), this);
        startTimerToEndRequest();
        startCameraIfMissedCall();
    }

    private void startCameraIfMissedCall() {
        if (getIntent().hasExtra("CAM_MIS")) {
            VibrationUtils.stopVibration(this);
            cancelTimerToServe();
            startQuickCountdownToPhoto();
            isPhotoTaken = true;
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

        clearFlags();
        VibrationUtils.stopVibration(this);
        releaseCamera();
    }

    private void initFlags() {
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

    private void initUi() {
        quickCountdownText = (TextView) findViewById(R.id.quick_count_down);
        quickCountdownText.setVisibility(View.INVISIBLE);
        callingUserInfo = findViewById(R.id.sender_info);
        buttonControls = findViewById(R.id.moving_layout);

        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPhotoCancelled) {
                    isPhotoCancelled = true;
                    cancelTimerToServe();
                    sendBusySenz();
                    VibrationUtils.stopVibration(SelfieCaptureActivity.this);
                    saveMissedSelfie();
                    SelfieCaptureActivity.this.finish();
                }
            }
        });
        startBtn = (ImageView) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VibrationUtils.stopVibration(SelfieCaptureActivity.this);
                if (!isPhotoTaken) {
                    cancelTimerToServe();
                    startQuickCountdownToPhoto();
                    isPhotoTaken = true;
                }
            }
        });

        rotateCamera = (ImageView) findViewById(R.id.rotate_camera);
        rotateCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFrontCam)
                    initCameraPreview(Camera.CameraInfo.CAMERA_FACING_BACK);
                else
                    initCameraPreview(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
        });

        callDeatilsLayout = (FrameLayout) findViewById(R.id.call_details_layout);
        selfiCallText = (TextView) findViewById(R.id.selfie_calling_text);
        callerNameText = (TextView) findViewById(R.id.caller_username);
        callerUsername = (TextView) findViewById(R.id.photo_request_user_name);

        selfiCallText.setTypeface(typeface);
        callerNameText.setTypeface(typeface);
        callerUsername.setTypeface(typeface);

        selfieImage = (ImageView) findViewById(R.id.selfie_image);
        AnimationDrawable anim = (AnimationDrawable) selfieImage.getBackground();
        anim.start();
    }

    private void initUser() {
        // secret user
        String username = getIntent().getStringExtra("USER");
        secretUser = new SenzorsDbSource(this).getSecretUser(username);
        String contactName = PhoneBookUtil.getContactName(this, secretUser.getPhone());

        callerNameText.setText(contactName);
        callerUsername.setText(contactName);
    }

    private void hideUiControls() {
        callDeatilsLayout.setVisibility(View.GONE);
        callingUserInfo.setVisibility(View.INVISIBLE);
        buttonControls.setVisibility(View.INVISIBLE);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            Log.d(TAG, "Stopping preview in SurfaceDestroyed().");
            mCamera.release();
        }
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
    private void initCameraPreview(int camFace) {
        if (mCameraPreview != null) {
            mCameraPreview.surfaceDestroyed(mCameraPreview.getHolder());
            mCameraPreview.getHolder().removeCallback(mCameraPreview);
            mCameraPreview.destroyDrawingCache();
            FrameLayout preview = (FrameLayout) findViewById(R.id.photo);
            preview.removeView(mCameraPreview);
            mCameraPreview.mCamera = null;
            mCameraPreview = null;

            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        // cam face
        isFrontCam = camFace == Camera.CameraInfo.CAMERA_FACING_FRONT;

        // render new preview
        try {
            mCamera = Camera.open(camFace);
            mCameraPreview = new CameraPreview(this, mCamera);

            FrameLayout preview = (FrameLayout) findViewById(R.id.photo);
            preview.addView(mCameraPreview);
        } catch (Exception e) {
            // cannot get camera or does not exist
            e.printStackTrace();
            Log.e(TAG, "No font cam");
        }
    }

    private void takePhoto() {
        quickCountdownText.setVisibility(View.INVISIBLE);
        AudioUtils.shootSound(this);
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                byte[] resizedImage = ImageUtils.compressImage(bytes);
                Log.d(TAG, "Compressed size: " + resizedImage.length / 1024);

                sendPhotoSenz(resizedImage, SelfieCaptureActivity.this);
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
                saveMissedSelfie();
                SelfieCaptureActivity.this.finish();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Time in count down -" + millisUntilFinished);
            }
        }.start();
    }

    private void saveMissedSelfie() {
        Long timestamp = (System.currentTimeMillis() / 1000);
        String uid = SenzUtils.getUid(this, timestamp.toString());
        Secret newSecret = new Secret("", BlobType.IMAGE, secretUser, true);
        newSecret.setTimeStamp(timestamp);
        newSecret.setId(uid);
        newSecret.setMissed(true);
        newSecret.setDeliveryState(DeliveryState.PENDING);
        new SenzorsDbSource(this).createSecret(newSecret);
    }

    private void sendBusySenz() {
        Long timestamp = (System.currentTimeMillis() / 1000);
        String uid = SenzUtils.getUid(this, timestamp.toString());

        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("uid", uid);
        senzAttributes.put("status", "CAM_BUSY");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
        send(_senz);
    }

    private void cancelTimerToServe() {
        if (cancelTimer != null)
            cancelTimer.cancel();
    }

    /**
     * Util methods
     */
    private void sendPhotoSenz(final byte[] image, final Context context) {
        // compose senz
        Long timestamp = (System.currentTimeMillis() / 1000);
        String uid = SenzUtils.getUid(this, timestamp.toString());

        // stream on senz
        // stream content
        // stream off senz
        Senz startStreamSenz = getStartStreamSenz(uid, timestamp);
        ArrayList<Senz> photoSenzList = getPhotoStreamSenz(image, context, uid, timestamp);
        Senz stopStreamSenz = getStopStreamSenz(uid, timestamp);

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
     * @param image   image content
     * @param context app context
     * @param uid     unique id
     * @return list of decomposed senz's
     */
    private ArrayList<Senz> getPhotoStreamSenz(byte[] image, Context context, String uid, Long timestamp) {
        String imageString = ImageUtils.encodeBitmap(image);

        Secret newSecret = new Secret("", BlobType.IMAGE, secretUser, false);
        newSecret.setTimeStamp(timestamp);
        newSecret.setId(uid);
        newSecret.setMissed(false);
        newSecret.setDeliveryState(DeliveryState.PENDING);
        new SenzorsDbSource(context).createSecret(newSecret);

        String imgName = uid + ".jpg";
        ImageUtils.saveImg(imgName, image);

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] packets = split(imageString, 1024);

        for (String packet : packets) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.STREAM;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", timestamp.toString());
            senzAttributes.put("cam", packet.trim());
            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
            senzList.add(_senz);
        }

        return senzList;
    }

    /**
     * Create start stream senz
     *
     * @return senz
     */
    private Senz getStartStreamSenz(String uid, Long timestamp) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("cam", "on");
        senzAttributes.put("uid", uid);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        return new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);
    }

    /**
     * Create stop stream senz
     *
     * @return senz
     */
    private Senz getStopStreamSenz(String uid, Long timestamp) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("cam", "off");
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

}