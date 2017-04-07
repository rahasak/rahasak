package com.score.rahasak.remote;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.CryptoUtils;
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

import io.kvh.media.amr.AmrDecoder;
import io.kvh.media.amr.AmrEncoder;


public class CallService extends Service {

    private static final String TAG = CallService.class.getName();

    public static final int SAMPLE_RATE = 44100;

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

        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        initPrefs(intent);
        initUdpSoc();
        initUdpConn();
        getAudioSettings();

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
    }

    private void initPrefs(Intent intent) {
        try {
            appUser = PreferenceUtils.getUser(this);
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        if (intent.hasExtra("USER"))
            secretUser = intent.getParcelableExtra("USER");

        secretKey = CryptoUtils.getSecretKey(secretUser.getSessionKey());
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
                enableEarpiece();
                startCall();
            } else if (senz.getAttributes().get("mic").equalsIgnoreCase("off")) {
                VibrationUtils.vibrate(this);
            }
        }
    }

    private void startCall() {
        calling = true;

        player.start();
        recorder.start();
    }

    private void endCall() {
        calling = false;
    }

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
    }

    private void getAudioSettings() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioMode = audioManager.getMode();
        ringMode = audioManager.getRingerMode();
        isSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
    }

    private void resetAudioSettings() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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

        Player() {
            thread = new Thread(this);

            int minBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                    AudioTrack.MODE_STREAM);
            Log.d(TAG, "min buffer size: " + minBufSize);
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
            streamTrack.play();
            final long amrState = AmrDecoder.init();

            try {
                short[] pcmframs = new short[160];
                byte[] message = new byte[64];
                while (calling) {
                    // listen for senz
                    DatagramPacket receivePacket = new DatagramPacket(message, message.length);
                    recvSoc.receive(receivePacket);
                    String msg = new String(message, 0, receivePacket.getLength());

                    // parser and obtain audio data
                    // play it
                    if (!msg.isEmpty()) {
                        // base64 decode
                        // decrypt
                        byte[] stream = CryptoUtils.decryptECB(secretKey, Base64.decode(msg, Base64.DEFAULT));

                        //Log.d(TAG, "Time +++++ " + System.currentTimeMillis());
                        // decode codec
                        AmrDecoder.decode(amrState, stream, pcmframs);
                        streamTrack.write(pcmframs, 0, pcmframs.length);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            AmrDecoder.exit(amrState);
            shutDown();
        }

        void shutDown() {
            if (streamTrack != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    streamTrack.pause();
                    streamTrack.flush();
                } else {
                    streamTrack.stop();
                }

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

        Recorder() {
            thread = new Thread(this);

            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
            audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    minBufSize * 10);
            AmrEncoder.init(0);
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
            audioRecorder.startRecording();
            int mode = AmrEncoder.Mode.MR795.ordinal();

            int encoded;
            short[] inBuf = new short[160];
            byte[] outBuf = new byte[32];
            while (calling) {
                // read to buffer
                // encode with codec
                audioRecorder.read(inBuf, 0, inBuf.length);
                encoded = AmrEncoder.encode(mode, inBuf, outBuf);

                try {
                    // encrypt
                    // base 64 encoded senz
                    String encodedStream = Base64.encodeToString(CryptoUtils.encryptECB(secretKey, outBuf, 0, encoded), Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

                    //Log.d(TAG, "Time ---- " + System.currentTimeMillis());
                    String senz = encodedStream + " @" + secretUser.getUsername() + " ^" + appUser.getUsername();
                    sendStream(senz);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            AmrEncoder.exit();
            shutDown();
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
            if (audioRecorder != null) {
                if (audioRecorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                    audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
            }
        }
    }
}
