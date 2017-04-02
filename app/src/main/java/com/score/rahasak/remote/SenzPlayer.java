package com.score.rahasak.remote;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.CryptoUtils;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.rahasak.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.crypto.SecretKey;

import io.kvh.media.amr.AmrDecoder;


public class SenzPlayer extends Service {

    private static final String TAG = SenzPlayer.class.getName();

    private User appUser;
    private SecretUser secretUser;
    private SecretKey secretKey;

    // we are listing for UDP socket
    private DatagramSocket socket;
    private InetAddress address;

    // keeps weather service already bound or not
    private boolean isServiceBound = false;

    // service interface
    private ISenzService senzService = null;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);
            isServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("TAG", "Disconnected from senz service");
            senzService = null;
            isServiceBound = false;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // bind with senz service
        if (!isServiceBound) {
            Intent serviceIntent = new Intent("com.score.rahasak.remote.SenzService");
            serviceIntent.setPackage(this.getPackageName());
            bindService(serviceIntent, senzServiceConnection, Context.BIND_AUTO_CREATE);
        }

        initPrefs(intent);
        initUdpSoc();
        initUdpConn();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unbind from service
        if (isServiceBound) {
            Log.d(TAG, "Unbind to senz service");
            unbindService(senzServiceConnection);

            isServiceBound = false;
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

        secretKey = CryptoUtils.getSecretKey(secretUser.getSessionKey());
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
                    String msg = SenzUtils.getStartStreamMsg(SenzPlayer.this, appUser.getUsername(), secretUser.getUsername());
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
     * Player thread
     */
    private class Player implements Runnable {

        private final Thread thread;

        private AudioTrack streamTrack;
        private boolean playing = true;

        Player() {
            thread = new Thread(this);

            int minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    AudioUtils.RECORDER_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    640,
                    AudioTrack.MODE_STREAM);
            Log.d(TAG, "min buffer size: " + minBufSize);
        }

        public void start() {
            thread.start();
            playing = true;
        }

        public void stop() {
            playing = false;
        }

        @Override
        public void run() {
            if (playing) {
                play();
            }
        }

        private void play() {
            streamTrack.play();
            final long amrState = AmrDecoder.init();

            try {
                short[] pcmframs = new short[160];
                byte[] message = new byte[64];
                while (playing) {
                    // listen for senz
                    DatagramPacket receivePacket = new DatagramPacket(message, message.length);
                    socket.receive(receivePacket);
                    String msg = new String(message, 0, receivePacket.getLength());

                    // parser and obtain audio data
                    // play it
                    if (!msg.isEmpty()) {
                        // base64 decode
                        // decrypt
                        byte[] stream = CryptoUtils.decryptECB(secretKey, Base64.decode(msg, Base64.DEFAULT));

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
    private class Recorder extends Thread {

    }
}
