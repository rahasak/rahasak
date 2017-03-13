package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.CryptoUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.crypto.SecretKey;

import io.kvh.media.amr.AmrDecoder;

public class StreamPlayer {

    private static final String TAG = StreamPlayer.class.getName();

    private Context context;
    private Player player;
    private SecretKey secretKey;

    // audio setting
    private int audioMode;
    private int ringMode;
    private boolean isSpeakerPhoneOn;

    private DatagramSocket socket;

    public StreamPlayer(Context context, DatagramSocket socket, String sessionKey) {
        this.context = context;
        this.socket = socket;
        this.secretKey = CryptoUtils.getSecretKey(sessionKey);

        player = new Player();
    }

    public void play() {
        getAudioSettings();
        enableEarpiece();
        player.start();
    }

    public void stop() {
        player.shutDown();
        resetAudioSettings();
    }

    private class Player extends Thread {
        private AudioTrack streamTrack;
        private int minBufSize;

        private boolean playing = true;

        private long amrState;

        Player() {
            minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    AudioUtils.RECORDER_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    640,
                    AudioTrack.MODE_STREAM);
            Log.d(TAG, "min buffer size: " + minBufSize);

            amrState = AmrDecoder.init();
        }

        @Override
        public void run() {
            if (playing) {
                play();
            }
        }

        private void play() {
            streamTrack.play();

            try {
                short[] pcmframs = new short[160];
                byte[] message = new byte[64];
                while (playing) {
                    // listen for senz
                    DatagramPacket receivePacket = new DatagramPacket(message, message.length);
                    socket.receive(receivePacket);
                    String msg = new String(message, 0, receivePacket.getLength());
                    Log.d(TAG, "stream received: " + msg);

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
        }

        void shutDown() {
            playing = false;

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

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
    }

    private void getAudioSettings() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioMode = audioManager.getMode();
        ringMode = audioManager.getRingerMode();
        isSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
    }

    private void resetAudioSettings() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(audioMode);
        audioManager.setRingerMode(ringMode);
        audioManager.setSpeakerphoneOn(isSpeakerPhoneOn);
    }

}
