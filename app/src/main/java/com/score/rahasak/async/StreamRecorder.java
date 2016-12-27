package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.remote.SenzService;
import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.SenzUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StreamRecorder {

    private Context context;
    private String from;
    private String to;

    private DatagramSocket socket;
    private InetAddress address;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);

    private Recorder recorder;

    public StreamRecorder(Context context, DatagramSocket socket, String from, String to) {
        this.context = context;
        this.socket = socket;
        this.from = from;
        this.to = to;

        recorder = new Recorder();
    }

    public void start() {
        recorder.start();
    }

    public void stop() {
        recorder.shutDown();
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
            byte[] buffer = new byte[minBufSize];
            while (recording) {
                Log.d("RECORD", minBufSize + "+++++++++");
                audioRecorder.read(buffer, 0, buffer.length);
                String stream = Base64.encodeToString(buffer, Base64.DEFAULT);

                // create datagram packet and send
                String datagram = SenzUtils.getSenzStream(stream, from, to);
                sendDatagram(datagram);
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

        private byte[] short2byte(short[] sData) {
            byte[] bytes = new byte[sData.length * 2];
            for (int i = 0; i < sData.length; i++) {
                bytes[i * 2] = (byte) (sData[i] & 0x00FF);
                bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
                sData[i] = 0;
            }

            return bytes;
        }

        private void sendDatagram(String datagram) {
            try {
                if (address == null) address = InetAddress.getByName(SenzService.STREAM_HOST);
                DatagramPacket sendPacket = new DatagramPacket(datagram.getBytes(), datagram.length(), address, SenzService.STREAM_PORT);
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
