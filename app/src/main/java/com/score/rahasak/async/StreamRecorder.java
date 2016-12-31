package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.CMG711;
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
    private SecretKey key;

    private DatagramSocket socket;
    private InetAddress address;

    private Recorder recorder;

    public StreamRecorder(Context context, String from, String to, SecretKey key) {
        this.context = context;
        this.from = from;
        this.to = to;
        this.key = key;

        recorder = new Recorder();
    }

    public void start() {
        recorder.start();
    }

    public void stop() {
        recorder.shutDown();
    }

    private class Recorder extends Thread {
        private CMG711 encoder = new CMG711();

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
            int mode = AmrEncoder.Mode.MR122.ordinal();

            int read, encoded;
            short[] inBuf = new short[160];
            byte[] outBuf = new byte[32];
            while (recording) {
                read = audioRecorder.read(inBuf, 0, inBuf.length);

                // encode with codec
                encoded = AmrEncoder.encode(mode, inBuf, outBuf);

                //Log.d("TAG", encoded + " -----");
                //Log.d("TAG", minBufSize + " ////");

                try {
                    // encrypt
                    // base 64 encoded senz
                    String encodedStream = Base64.encodeToString(RSAUtils.encrypt(key, outBuf, 0, encoded), Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");
                    //String senz = SenzUtils.getSenzStream(encodedStream, from, to);
                    //Log.d("TAG", senz.length() + " ---");

                    //Log.d("TAG", senz.length() + " ++++");
                    sendDatagram(encodedStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        void shutDown() {
            recording = false;

            if (audioRecorder != null) {
                if (audioRecorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                    audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
            }

            AmrEncoder.exit();
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
