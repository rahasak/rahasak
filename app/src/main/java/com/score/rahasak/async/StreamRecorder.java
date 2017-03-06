package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.RSAUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.crypto.SecretKey;

import io.kvh.media.amr.AmrEncoder;

public class StreamRecorder {

    private Context context;
    private String from;
    private String to;
    private byte[] salt;
    private SecretKey secretKey;

    private DatagramSocket socket;
    private InetAddress address;

    private Recorder recorder;

    public StreamRecorder(Context context, String from, String to, String sessionKey) {
        this.context = context;
        this.from = from;
        this.to = to;
        this.secretKey = RSAUtils.getSecretKey(sessionKey);
        this.salt = sessionKey.substring(0, 7).toUpperCase().getBytes();

        recorder = new Recorder();
    }

    public void start() {
        recorder.start();
    }

    public void stop() {
        recorder.shutDown();
    }

    private class Recorder extends Thread {
        private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        private AudioRecord audioRecorder;
        private int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);

        boolean recording = true;

        @Override
        public void run() {
            if (recording) {
                startRecord();
                record();
            }
        }

        private void startRecord() {
            audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    AudioUtils.RECORDER_SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    minBufSize * 10);
            audioRecorder.startRecording();
        }

        private void record() {
            AmrEncoder.init(0);
            int mode = AmrEncoder.Mode.MR795.ordinal();

            int encoded;
            short[] inBuf = new short[160];
            byte[] outBuf = new byte[32];
            while (recording) {
                // read to buffer
                // encode with codec
                audioRecorder.read(inBuf, 0, inBuf.length);
                encoded = AmrEncoder.encode(mode, inBuf, outBuf);

                try {
                    // encrypt
                    // base 64 encoded senz
                    String encodedStream = Base64.encodeToString(RSAUtils.encrypt(secretKey, salt, outBuf, 0, encoded), Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

                    String senz = encodedStream + " @" + to + " ^" + from;
                    Log.d("TAG", senz + " ---");
                    sendDatagram(senz);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            AmrEncoder.exit();
        }

        void shutDown() {
            recording = false;

            if (audioRecorder != null) {
                if (audioRecorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                    audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
            }
        }
    }

    private void sendDatagram(String senz) {
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

}
