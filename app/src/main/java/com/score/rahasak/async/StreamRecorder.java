package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import com.score.rahasak.interfaces.IStreamListener;
import com.score.rahasak.utils.AudioUtils;

public class StreamRecorder {

    private Context context;
    private IStreamListener streamListener;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);

    private AudioRecord audioRecorder;
    private Recorder recorder;

    public StreamRecorder(Context context, IStreamListener listener) {
        this.context = context;
        this.streamListener = listener;

        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat, minBufSize * 10);
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
            byte[] buffer = new byte[minBufSize];
            while (recording) {
                audioRecorder.read(buffer, 0, buffer.length);

                String stream = Base64.encodeToString(buffer, Base64.DEFAULT);
                streamListener.onStream(stream);
            }
        }
    }
}
