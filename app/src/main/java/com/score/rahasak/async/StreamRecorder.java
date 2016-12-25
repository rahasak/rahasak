package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import com.score.rahasak.interfaces.IStreamListener;
import com.score.rahasak.utils.AudioUtils;

/**
 * Created by eranga on 12/25/16.
 */

public class StreamRecorder {

    private Context context;
    private IStreamListener listener;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat);

    private StringBuffer streamBuffer;

    private AudioRecord recorder;
    //private Reader reader;
    private Writer writer;

    public StreamRecorder(Context context, IStreamListener listener) {
        this.context = context;
        this.listener = listener;

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioUtils.RECORDER_SAMPLE_RATE, channelConfig, audioFormat, minBufSize * 10);
        streamBuffer = new StringBuffer();
        //reader = new Reader();
        writer = new Writer();
    }

    public void start() {
        recorder.startRecording();
        //reader.start();
        writer.start();
    }

    public void stop() {
        //reader.shutDown();
        writer.shutDown();
    }

    private class Reader extends Thread {
        boolean reading = true;

        void shutDown() {
            reading = false;

            // TODO send remaining data in buffer
        }

        @Override
        public void run() {
            if (reading) read();
        }

        private void read() {
            while (reading) {
                if (streamBuffer.length() >= 1024) {
                    int index = 1024;
                    String stream = streamBuffer.substring(0, 1024);
                    String encodedStream = Base64.encodeToString(stream.getBytes(), Base64.DEFAULT);

                    streamBuffer.delete(0, index);

                    listener.onStream(encodedStream);
                }
            }
        }
    }

    private class Writer extends Thread {
        boolean writing = true;

        void shutDown() {
            writing = false;
            if (recorder != null) {
                if (recorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                    recorder.stop();
                recorder.release();
                recorder = null;
            }
        }

        @Override
        public void run() {
            if (writing) {
                recorder.startRecording();
                write();
            }
        }

        private void write() {
            byte[] buffer = new byte[minBufSize];
            while (writing) {
                recorder.read(buffer, 0, buffer.length);
                //streamBuffer.append(new String(buffer));

                String stream = Base64.encodeToString(buffer, Base64.DEFAULT);
                listener.onStream(stream);
            }
        }
    }
}
