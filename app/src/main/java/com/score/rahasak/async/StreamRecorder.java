package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.interfaces.IStreamListener;
import com.score.rahasak.utils.AudioUtils;

public class StreamRecorder {

    private static final String TAG = StreamRecorder.class.getName();

    private Context context;
    private IStreamListener streamListener;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSizeInBytes, bufferSizeInShorts;
    private int shortsRead;

    private AudioRecord audioRecorder;
    private Recorder recorder;

    public StreamRecorder(Context context, IStreamListener listener) {
        this.context = context;
        this.streamListener = listener;

        bufferSizeInBytes = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);
        bufferSizeInShorts = bufferSizeInBytes / 2;

        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat, bufferSizeInBytes);
        recorder = new Recorder();
    }

    public void start() {
        audioRecorder.startRecording();
        recorder.start();
    }

    public void stop() {
        recorder.shutDown();

        if (audioRecorder != null) {
            if (audioRecorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
        }
    }

    private class Recorder extends Thread {
        boolean recording = true;

        void shutDown() {
            recording = false;
        }

        @Override
        public void run() {
            if (recording) {
                record();
            }
        }

        private void record() {
            short[] shortBuffer = new short[bufferSizeInShorts];
            while (recording) {
                shortsRead = audioRecorder.read(shortBuffer, 0, bufferSizeInShorts);
                if (shortsRead == AudioRecord.ERROR_BAD_VALUE || shortsRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Error reading from mic");
                    shutDown();
                    break;
                }

                String stream = Base64.encodeToString(short2byte(shortBuffer), Base64.DEFAULT);
                streamListener.onStream(stream);
            }
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
}
