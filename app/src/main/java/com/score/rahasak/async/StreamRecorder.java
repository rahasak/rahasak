package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.SenzBuffer;
import com.score.rahasak.utils.SenzUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.crypto.SecretKey;

public class StreamRecorder {

    private Context context;
    private String from;
    private String to;
    private SecretKey key;

    private DatagramSocket socket;
    private InetAddress address;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);

    private Recorder recorder;
    private Writer writer;
    private SenzBuffer senzBuffer;

    public StreamRecorder(Context context, String from, String to, SecretKey key) {
        this.context = context;
        this.from = from;
        this.to = to;
        this.key = key;

        recorder = new Recorder();
        writer = new Writer();
        senzBuffer = new SenzBuffer();
    }

    public void start() {
        recorder.start();
        writer.start();
    }

    public void stop() {
        recorder.shutDown();
        writer.shutDown();
    }

    private class Recorder extends Thread {
        private AudioRecord audioRecorder;
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
            byte[] buf = new byte[minBufSize];
            while (recording) {
                audioRecorder.read(buf, 0, buf.length);

                // add byte data directly
                senzBuffer.put(buf);
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
        }
    }

    private class Writer extends Thread {
        boolean writing = true;

        @Override
        public void run() {
            if (writing) {
                write();
            }
        }

        private void write() {
            while (writing) {
                if (senzBuffer.size() > 500) {
                    //byte[] stream = AudioUtils.compress(senzBuffer.get(0, 500));
                    byte[] stream = senzBuffer.get(0, 500);

                    String encodedStream = Base64.encodeToString(stream, Base64.DEFAULT);
                    String senz = SenzUtils.getSenzStream(encodedStream, from, to);

                    sendDatagram(senz);
                }
            }
        }

        void shutDown() {
            writing = false;
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

}
