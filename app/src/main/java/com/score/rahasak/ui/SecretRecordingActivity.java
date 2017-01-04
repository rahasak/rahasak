package com.score.rahasak.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.async.StreamPlayer;
import com.score.rahasak.async.StreamRecorder;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.pojo.Secret;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.PhoneBookUtil;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.rahasak.utils.RSAUtils;
import com.score.rahasak.utils.SenzUtils;
import com.score.rahasak.utils.VibrationUtils;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.crypto.SecretKey;

public class SecretRecordingActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = SecretRecordingActivity.class.getName();

    private static final int TIME_TO_SERVE_REQUEST = 15000;

    protected Typeface typeface;

    private FrameLayout screenOff;
    private FrameLayout callingUser;
    private RelativeLayout rootView;
    private TextView callingText;
    private TextView callingHeaderText;
    private TextView callingUsernameText;
    private ImageView waitingIcon;
    private CircularImageView cancelBtn;
    private CircularImageView startBtn;
    private CircularImageView endBtn;

    private User appUser;
    private SecretUser secretUser;
    private SecretKey key;

    private SensorManager sensorManager;
    private Sensor proximitySensor;

    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

    // we are listing for UDP socket
    private DatagramSocket socket;
    private InetAddress address;

    private StreamRecorder streamRecorder;
    private StreamPlayer streamPlayer;

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
                    default:
                        break;
                }
            }
        }
    };

    private CountDownTimer requestTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
        @Override
        public void onFinish() {
            sendSenz(SenzUtils.getMicBusySenz(SecretRecordingActivity.this, secretUser));
            stopVibrations();
            SecretRecordingActivity.this.finish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secret_recording_activity_layout);

        initUi();
        initStatusBar();
        initUser();
        initSensor();
        setupWakeLock();
        startVibrations();
        startTimerToEndRequest();
        initCall();
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
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        clearFlags();
    }

    @Override
    public void onBackPressed() {
        // disable back button
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        WindowManager.LayoutParams params = this.getWindow().getAttributes();
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] == 0) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                params.screenBrightness = 0;
                getWindow().setAttributes(params);
                screenOff.setVisibility(View.VISIBLE);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                params.screenBrightness = -1f;
                getWindow().setAttributes(params);
                screenOff.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            // TODO
        } else if (senz.getAttributes().containsKey("mic")) {
            if (senz.getAttributes().get("mic").equalsIgnoreCase("off")) {
                cancelTimerToServe();
                endCall();
                SecretRecordingActivity.this.finish();
            }
        }
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        screenOff = (FrameLayout) findViewById(R.id.screen_off);
        rootView = (RelativeLayout) findViewById(R.id.moving_layout);
        callingUser = (FrameLayout) findViewById(R.id.calling_user);
        callingHeaderText = (TextView) findViewById(R.id.mic_calling_text);
        callingUsernameText = (TextView) findViewById(R.id.mic_username);
        callingText = (TextView) findViewById(R.id.calling_text);

        callingHeaderText.setTypeface(typeface);
        callingUsernameText.setTypeface(typeface);
        callingText.setTypeface(typeface);

        waitingIcon = (ImageView) findViewById(R.id.selfie_image);
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();

        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTimerToServe();
                sendSenz(SenzUtils.getMicBusySenz(SecretRecordingActivity.this, secretUser));
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
                startCall();
            }
        });

        endBtn = (CircularImageView) findViewById(R.id.end_call);
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
                SecretRecordingActivity.this.finish();
            }
        });
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
    }

    private void initUser() {
        // app user
        try {
            appUser = PreferenceUtils.getUser(this);
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        // secret user
        String username = getIntent().getStringExtra("USER");
        secretUser = new SenzorsDbSource(this).getSecretUser(username);
        if (secretUser.getSessionKey() != null)
            key = RSAUtils.getSecretKey(secretUser.getSessionKey());

        callingUsernameText.setText(PhoneBookUtil.getContactName(this, secretUser.getPhone()));
        if (secretUser.getImage() != null) {
            BitmapDrawable drawable = new BitmapDrawable(getResources(), ImageUtils.decodeBitmap(secretUser.getImage()));
            callingUser.setBackground(drawable);
        }
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    private void initCall() {
        // connect to UDP
        initUdpSoc();
        initUdpConn();
    }

    private void initUdpSoc() {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Socket already initialized");
        }
    }

    /**
     * Initialize/Create UDP socket
     */
    private void initUdpConn() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // connect
                    if (address == null)
                        address = InetAddress.getByName(SenzService.STREAM_HOST);

                    // send init message
                    String msg = SenzUtils.getStartStreamMsg(SecretRecordingActivity.this, appUser.getUsername(), secretUser.getUsername());
                    if (msg != null) {
                        DatagramPacket sendPacket = new DatagramPacket(msg.getBytes(), msg.length(), address, SenzService.STREAM_PORT);
                        socket.send(sendPacket);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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

    private void startCall() {
        // set up ui
        rootView.setVisibility(View.GONE);
        callingText.setVisibility(View.VISIBLE);
        endBtn.setVisibility(View.VISIBLE);

        // send mic on senz back
        Senz senz = SenzUtils.getMicOnSenz(this, secretUser);
        sendSenz(senz);

        // start recorder
        if (streamRecorder == null)
            streamRecorder = new StreamRecorder(this, appUser.getUsername(), secretUser.getUsername(), key);
        streamRecorder.start();

        // start player
        if (streamPlayer == null)
            streamPlayer = new StreamPlayer(this, socket, key);
        streamPlayer.play();
    }

    private void endCall() {
        stopVibrations();

        if (streamRecorder != null)
            streamRecorder.stop();

        if (streamPlayer != null)
            streamPlayer.stop();

        // send mic off senz
        // send stream off
        sendSenz(SenzUtils.getMicOffSenz(this, secretUser));
        sendDatagram(SenzUtils.getEndStreamMsg(this, appUser.getUsername(), secretUser.getUsername()));
    }

    private void sendSenz(Senz senz) {
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

    private void saveMissedCall() {
        Secret newSecret = new Secret("", BlobType.SOUND, secretUser, true);
        Long timeStamp = System.currentTimeMillis() / 1000;
        newSecret.setTimeStamp(timeStamp);
        newSecret.setId(SenzUtils.getUid(this, timeStamp.toString()));
        newSecret.setDeliveryState(DeliveryState.PENDING);
        new SenzorsDbSource(this).createSecret(newSecret);
    }

    private void sendDatagram(final String senz) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (address == null)
                        address = InetAddress.getByName(SenzService.STREAM_HOST);
                    if (socket == null)
                        socket = new DatagramSocket();
                    DatagramPacket sendPacket = new DatagramPacket(senz.getBytes(), senz.length(), address, SenzService.STREAM_PORT);
                    socket.send(sendPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}







