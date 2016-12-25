package com.score.rahasak.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Lakmal on 8/28/16.
 */
public class AudioRecorder {
    private static final String TAG = AudioRecorder.class.getName();
    private static final int BufferElements2Rec = 1024;
    private static final int BytesPerElement = 10;

    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);

    private AudioRecord recorder = null;
    private boolean isRecording = false;

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void startRecording() {
//        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                AudioUtils.RECORDER_SAMPLE_RATE,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                BufferElements2Rec * BytesPerElement);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
        recorder.startRecording();
        isRecording = true;

        // record asynchronously
        new Thread(new Runnable() {
            public void run() {
                writeAudioToBuffer();
            }
        }).start();
    }

    private void writeAudioToBuffer() {
        byte[] buffer = new byte[minBufSize];

        while (isRecording) {
            recorder.read(buffer, 0, buffer.length);
            try {
                baos.write(buffer);
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording() {
        try {
            if (recorder != null) {
                isRecording = false;
                if (recorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                    recorder.stop();
                recorder.release();
                recorder = null;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error in closing record " + ex);
        }
    }

    public ByteArrayOutputStream getRecording() {
        return baos;
    }

    private static byte[] short2byte(short[] sData) {
        byte[] bytes = new byte[sData.length * 2];
        for (int i = 0; i < sData.length; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }

        return bytes;
    }

}
