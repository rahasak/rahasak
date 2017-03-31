package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.CryptoUtils;

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
    private SecretKey secretKey;

    private DatagramSocket socket;
    private InetAddress address;

    private AudioRecord audioRecorder;
    private boolean recording;
    private boolean sending;

    public StreamRecorder(Context context, String from, String to, String sessionKey) {
        this.context = context;
        this.from = from;
        this.to = to;
        this.secretKey = CryptoUtils.getSecretKey(sessionKey);

        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AudioUtils.RECORDER_SAMPLE_RATE,
                channelConfig,
                audioFormat,
                minBufSize * 10);
        AmrEncoder.init(0);

        recording = true;
        sending = false;
        new Recorder().start();
    }

    public void start() {
        //recording = true;
        //new Recorder().start();
        sending = true;
    }

    public void stop() {
        recording = false;
    }

    private class Recorder extends Thread {

        @Override
        public void run() {
            record();
        }

        private void record() {
            audioRecorder.startRecording();
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
                    String encodedStream = Base64.encodeToString(CryptoUtils.encryptECB(secretKey, outBuf, 0, encoded), Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

                    String senz = encodedStream + " @" + to + " ^" + from;
                    sendDatagram(senz);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            AmrEncoder.exit();
            shutDown();
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
