package com.score.rahasak.remote;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.ui.SecretCallActivity;
import com.score.rahasak.ui.SecretCallAnswerActivity;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.CryptoUtils;
import com.score.rahasak.utils.NotificationUtils;
import com.score.rahasak.utils.OpusDecoder;
import com.score.rahasak.utils.OpusEncoder;
import com.score.rahasak.utils.PhoneBookUtil;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.rahasak.utils.SenzUtils;
import com.score.rahasak.utils.VibrationUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.crypto.SecretKey;


public class CallService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = CallService.class.getName();

    public static final int SAMPLE_RATE = 8000;
    public static final int FRAME_SIZE = 160;
    public static final int BUF_SIZE = FRAME_SIZE;

    private String activity;
    private User appUser;
    private SecretUser secretUser;
    private SecretKey secretKey;

    // current audio setting
    private int audioMode;
    private int ringMode;
    private boolean isSpeakerPhoneOn;

    // we are listing for UDP socket
    private InetAddress address;
    private DatagramSocket recvSoc;
    private DatagramSocket sendSoc;

    // audio
    private AudioManager audioManager;

    // crypto
    private CryptoManager cryptoManager;

    // player/recorder and state
    private Player player;
    private Recorder recorder;

    private boolean calling;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        initPrefs(intent);
        initCrypto();
        initUdpSoc();
        initUdpConn();
        getAudioSettings();
        startForegroundCall();
        audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);

        player = new Player();
        recorder = new Recorder();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        endCall();
        clrUdpConn();
        unregisterReceiver(senzReceiver);
        resetAudioSettings();
        audioManager.abandonAudioFocus(this);
        stopForeground(true);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange <= 0) {
            // loss audio focus
            // stop service
            stopSelf();
        }
    }

    private void initPrefs(Intent intent) {
        try {
            appUser = PreferenceUtils.getUser(this);
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        if (intent.hasExtra("USER"))
            secretUser = intent.getParcelableExtra("USER");

        if (intent.hasExtra("ACTIVITY"))
            activity = intent.getStringExtra("ACTIVITY");
        else
            activity = SecretCallActivity.class.getName();

        secretKey = CryptoUtils.getSecretKey(secretUser.getSessionKey());
    }

    private void initCrypto() {
        try {
            cryptoManager = CryptoManager.getInstance();
            cryptoManager.initCiphers(secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startForegroundCall() {
        Intent notificationIntent = new Intent(this, activity.equalsIgnoreCase(SecretCallActivity.class.getName()) ? SecretCallActivity.class : SecretCallAnswerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = PhoneBookUtil.getContactName(CallService.this, secretUser.getPhone());
        String message = "Calling...";
        Notification notification = NotificationUtils.getCallNotification(CallService.this, title, message, intent);
        startForeground(SenzNotificationManager.CALL_NOTIFICATION_ID, notification);
    }

    private void initUdpSoc() {
        if (recvSoc == null || recvSoc.isClosed()) {
            try {
                recvSoc = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Socket already initialized");
        }
    }

    private void initUdpConn() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // connect
                    if (address == null)
                        address = InetAddress.getByName(SenzService.STREAM_HOST);

                    // send init message
                    String msg = SenzUtils.getStartStreamMsg(CallService.this, appUser.getUsername(), secretUser.getUsername());
                    if (msg != null) {
                        DatagramPacket sendPacket = new DatagramPacket(msg.getBytes(), msg.length(), address, SenzService.STREAM_PORT);
                        recvSoc.send(sendPacket);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void clrUdpConn() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // send mic off
                    String senz = SenzUtils.getEndStreamMsg(CallService.this, appUser.getUsername(), secretUser.getUsername());
                    DatagramPacket sendPacket = new DatagramPacket(senz.getBytes(), senz.length(), address, SenzService.STREAM_PORT);
                    recvSoc.send(sendPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("mic")) {
            if (senz.getAttributes().get("mic").equalsIgnoreCase("on")) {
                VibrationUtils.vibrate(this);
                startCall();
            } else if (senz.getAttributes().get("mic").equalsIgnoreCase("off")) {
                VibrationUtils.vibrate(this);
            }
        }
    }

    private void startCall() {
        calling = true;

        recorder.start();
        player.start();

        AudioUtils.enableEarpiece(CallService.this);
    }

    private void endCall() {
        calling = false;
    }

    private void getAudioSettings() {
        audioMode = audioManager.getMode();
        ringMode = audioManager.getRingerMode();
        isSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
    }

    private void resetAudioSettings() {
        audioManager.setMode(audioMode);
        audioManager.setRingerMode(ringMode);
        audioManager.setSpeakerphoneOn(isSpeakerPhoneOn);
    }

    /**
     * Player thread
     */
    private class Player implements Runnable {

        private final Thread thread;

        private AudioTrack streamTrack;

        private OpusDecoder opusDecoder;

        Player() {
            thread = new Thread(this);

            int minBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                    AudioTrack.MODE_STREAM);
            Log.d(TAG, "AudioTrack min buffer size: ---- " + minBufSize);

            // init opus decoder
            opusDecoder = new OpusDecoder();
            opusDecoder.init(SAMPLE_RATE, 1, FRAME_SIZE);
        }

        public void start() {
            thread.start();
        }

        @Override
        public void run() {
            if (calling) {
                play();
            }
        }

        private void play() {
            Log.d(TAG, "Player started --- " + calling);
            streamTrack.play();

            try {
                short[] pcmframs = new short[BUF_SIZE];
                byte[] message = new byte[BUF_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(message, message.length);

                while (calling) {
                    // listen for senz
                    recvSoc.receive(receivePacket);
                    String msg = new String(message, 0, receivePacket.getLength());

                    // parser and obtain audio data
                    // play it
                    if (!msg.isEmpty()) {
                        // base64 decode
                        // decrypt
                        // decode codec
                        opusDecoder.decode(cryptoManager.decrypt(Base64.decode(msg, Base64.DEFAULT)), pcmframs);
                        streamTrack.write(pcmframs, 0, pcmframs.length);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Player error --- " + calling);
            }

            shutDown();
            opusDecoder.close();
        }

        void shutDown() {
            Log.d(TAG, "Player finished --- " + calling);
            if (streamTrack != null) {
                streamTrack.pause();
                streamTrack.flush();
                streamTrack.release();

                streamTrack = null;
            }
        }
    }

    /**
     * Recorder thread
     */
    private class Recorder implements Runnable {
        private final Thread thread;

        private AudioRecord audioRecorder;

        private OpusEncoder opusEncoder;

        Recorder() {
            thread = new Thread(this);

            int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize);

            Log.d(TAG, "Recorder min buffer size: ---- " + minBufSize);

            // init opus encoder
            opusEncoder = new OpusEncoder();
            opusEncoder.init(SAMPLE_RATE, 1, FRAME_SIZE);
        }

        public void start() {
            thread.start();
        }

        @Override
        public void run() {
            if (calling) {
                record();
            }
        }

        private void record() {
            Log.d(TAG, "Recorder started --- " + calling);
            audioRecorder.startRecording();

            // enable
            // 1. AutomaticGainControl
            // 2. NoiseSuppressor
            // 3. AcousticEchoCanceler
            AudioUtils.enableAGC(audioRecorder.getAudioSessionId());
            AudioUtils.enableNS(audioRecorder.getAudioSessionId());
            AudioUtils.enableAEC(audioRecorder.getAudioSessionId());

            int encoded;
            short[] inBuf = new short[BUF_SIZE];
            byte[] outBuf = new byte[BUF_SIZE];

            while (calling) {
                // read to buffer
                // encode with codec
                audioRecorder.read(inBuf, 0, inBuf.length);
                encoded = opusEncoder.encode(inBuf, outBuf);

                try {
                    // encrypt
                    // base 64 encoded senz
                    sendStream(Base64.encodeToString(cryptoManager.encrypt(outBuf, 0, encoded), Base64.DEFAULT).
                            replaceAll("\n", "").replaceAll("\r", "") + " @" + secretUser.getUsername() + " ^" + appUser.getUsername());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Recorder error --- " + calling);
                }
            }

            shutDown();
            opusEncoder.close();
        }

        private void sendStream(String senz) {
            try {
                if (sendSoc == null)
                    sendSoc = new DatagramSocket();
                DatagramPacket sendPacket = new DatagramPacket(senz.getBytes(), senz.length(), address, SenzService.STREAM_PORT);
                sendSoc.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void shutDown() {
            Log.d(TAG, "Recorder finished --- " + calling);
            if (audioRecorder != null) {
                if (audioRecorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                    audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
            }
        }
    }

}
