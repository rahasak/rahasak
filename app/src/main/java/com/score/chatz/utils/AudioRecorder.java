package com.score.chatz.utils;

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
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BufferElements2Rec = 1024;
    private static final int BytesPerElement = 2;

    private AudioRecord recorder = null;
    private boolean isRecording = false;

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AudioUtils.RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
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
        short sData[] = new short[BufferElements2Rec];

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                // writes the data to file from buffer
                // stores the voice buffer
                byte bData[] = short2byte(sData);
                buffer.write(bData);
                buffer.close();
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
        return buffer;
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
