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
import android.util.Base64;
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
import com.score.rahasak.async.StreamPlayer;
import com.score.rahasak.async.StreamRecorder;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;
import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.pojo.Secret;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.rahasak.utils.SenzParser;
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

public class SecretRecordingActivity extends AppCompatActivity {

    private static final String TAG = SecretRecordingActivity.class.getName();

    private static final int TIME_TO_SERVE_REQUEST = 15000;

    protected Typeface typeface;

    private RelativeLayout rootView;
    private FrameLayout callingUser;
    private TextView callingText;
    private TextView callingHeaderText;
    private TextView callingUsernameText;
    private ImageView waitingIcon;
    private CircularImageView cancelBtn;
    private CircularImageView startBtn;

    private User appUser;
    private SecretUser secretUser;

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

    private CountDownTimer requestTimer = new CountDownTimer(TIME_TO_SERVE_REQUEST, TIME_TO_SERVE_REQUEST) {
        @Override
        public void onFinish() {
            sendSenz(SenzUtils.getMicBusySenz(SecretRecordingActivity.this, secretUser));
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
        initStatusBar();
        initUser();
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
    protected void onDestroy() {
        super.onDestroy();

        stopVibrations();
        clearFlags();
        endCall();
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
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();

        cancelBtn = (CircularImageView) findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVibrations();
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

        callingUsernameText.setText(" @" + secretUser.getUsername());
        if (secretUser.getImage() != null) {
            BitmapDrawable drawable = new BitmapDrawable(getResources(), new ImageUtils().decodeBitmap(secretUser.getImage()));
            callingUser.setBackground(drawable);
        }
    }

    private void initCall() {
        // connect to UDP
        initUdpSoc();
        initUdpConn();
        initUdpLsn();
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
                    String msg = SenzUtils.getStartStreamMsg(SecretRecordingActivity.this, appUser.getUsername(), "streamswitch");
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

    /**
     * Start thread for listen to UDP socket, all the incoming messages receives from
     * here, when message receives it should be broadcast or delegate to appropriate message
     * handler
     */
    private void initUdpLsn() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] message = new byte[1024];

                    while (true) {
                        // listen for senz
                        DatagramPacket receivePacket = new DatagramPacket(message, message.length);
                        socket.receive(receivePacket);
                        String msg = new String(message, 0, receivePacket.getLength());

                        Log.d(TAG, "Stream received: " + msg);

                        // parser and obtain audio data
                        // play it
                        if (!msg.isEmpty()) {
                            if (streamPlayer != null) {
                                Senz senz = SenzParser.parse(msg);
                                if (senz.getAttributes().containsKey("mic")) {
                                    String data = senz.getAttributes().get("mic");
                                    streamPlayer.onStream(Base64.decode(data, Base64.DEFAULT));
                                }
                            }
                        }
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

        // send mic on senz back
        Senz senz = SenzUtils.getMicOnSenz(this, secretUser);
        sendSenz(senz);

        // start recorder
        if (streamRecorder == null)
            streamRecorder = new StreamRecorder(this, appUser.getUsername(), secretUser.getUsername());
        streamRecorder.start();

        // start player
        if (streamPlayer == null)
            streamPlayer = new StreamPlayer(this);
        streamPlayer.play();
    }

    private void endCall() {
        if (streamRecorder != null)
            streamRecorder.stop();

        if (streamPlayer != null)
            streamPlayer.stop();
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

}







