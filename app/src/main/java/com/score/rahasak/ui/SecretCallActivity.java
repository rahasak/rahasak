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
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.remote.CallService;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.PhoneBookUtil;
import com.score.rahasak.utils.SenzUtils;
import com.score.rahasak.utils.VibrationUtils;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;

public class SecretCallActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = SecretCallActivity.class.getName();

    private Typeface typeface;

    private FrameLayout screenOff;
    private FrameLayout callingUser;
    private View loadingView;
    private TextView playingText;
    private TextView usernameText;
    private TextView callingText;
    private ImageView waitingIcon;
    private ImageView endBtn;

    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

    // for ringing
    private Ringer ringer;
    private boolean ringing;

    private SecretUser secretUser;

    private SensorManager sensorManager;
    private Sensor proximitySensor;

    // service connection
    protected ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);
            isServiceBound = true;

            // send init mic senz from here
            Senz senz = SenzUtils.getInitMicSenz(SecretCallActivity.this, secretUser);
            sendSenz(senz);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secret_call_activity_layout);

        initUi();
        initStatusBar();
        initUser();
        initSensor();
        initCall();

        // ring
        AudioUtils.enableEarpiece(this);
        ringer = new Ringer();
        startRing();
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
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
        sensorManager.unregisterListener(this);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // stop service
        stopService(new Intent(this, CallService.class));
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        screenOff = (FrameLayout) findViewById(R.id.screen_off);
        callingUser = (FrameLayout) findViewById(R.id.calling_user);
        loadingView = findViewById(R.id.mic_loading_view);

        waitingIcon = (ImageView) findViewById(R.id.selfie_image);
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();

        playingText = (TextView) findViewById(R.id.mic_playingText);
        usernameText = (TextView) findViewById(R.id.mic_username);
        callingText = (TextView) findViewById(R.id.mic_calling_text);

        usernameText.setTypeface(typeface, Typeface.BOLD);
        callingText.setTypeface(typeface, Typeface.NORMAL);
        playingText.setTypeface(typeface, Typeface.NORMAL);

        endBtn = (CircularImageView) findViewById(R.id.end_call);
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ringing) {
                    // ringing means call not started yet, send busy status
                    endRing();
                    sendSenz(SenzUtils.getMicBusySenz(SecretCallActivity.this, secretUser));
                } else {
                    // not ringing means call started, so send mic off
                    sendSenz(SenzUtils.getMicOffSenz(SecretCallActivity.this, secretUser));
                }

                SecretCallActivity.this.finish();
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
        secretUser = getIntent().getParcelableExtra("USER");
        usernameText.setText(PhoneBookUtil.getContactName(this, secretUser.getPhone()));

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
        // start service
        Intent intent = new Intent(this, CallService.class);
        intent.putExtra("USER", secretUser);
        intent.putExtra("ACTIVITY", SecretCallActivity.class.getName());
        startService(intent);
    }

    private void startRing() {
        ringing = true;
        ringer.start();
    }

    private void endRing() {
        ringing = false;
        ringer.stop();
    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            if (senz.getAttributes().get("status").equalsIgnoreCase("BUSY")) {
                // user busy
                Toast.makeText(this, "User busy", Toast.LENGTH_LONG).show();
                endRing();
                SecretCallActivity.this.finish();
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("offline")) {
                // offline
                Toast.makeText(this, "User offline", Toast.LENGTH_LONG).show();
                endRing();
                SecretCallActivity.this.finish();
            }
        } else if (senz.getAttributes().containsKey("mic")) {
            if (senz.getAttributes().get("mic").equalsIgnoreCase("on")) {
                VibrationUtils.vibrate(this);
                endRing();
                startCall();
            } else if (senz.getAttributes().get("mic").equalsIgnoreCase("off")) {
                SecretCallActivity.this.finish();
            }
        }
    }

    private void startCall() {
        loadingView.setVisibility(View.INVISIBLE);
        playingText.setVisibility(View.VISIBLE);
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

    private class Ringer implements Runnable {
        private final Thread thread;

        private MediaPlayer mediaPlayer;

        Ringer() {
            thread = new Thread(this);
            mediaPlayer = MediaPlayer.create(SecretCallActivity.this, R.raw.ring);
            mediaPlayer.setLooping(true);
        }

        public void start() {
            //thread.start();
        }

        public void stop() {
            mediaPlayer.stop();
        }

        @Override
        public void run() {
            mediaPlayer.start();
        }
    }

}