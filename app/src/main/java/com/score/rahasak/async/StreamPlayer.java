package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.utils.AudioUtils;

/**
 * Created by eranga on 12/25/16.
 */

public class StreamPlayer {

    private Context context;
    private StringBuffer buffer;
    private StreamListener listener;

    private AudioTrack streamTrack;

    public StreamPlayer(Context context) {
        this.context = context;
        buffer = new StringBuffer();
        listener = new StreamListener();

        int size = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                AudioUtils.RECORDER_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                size,
                AudioTrack.MODE_STREAM);
    }

    public void play() {
        listener.start();
        streamTrack.play();
    }

    public void onStream(String stream) {
        buffer.append(stream);
    }

    public void stop() {
        listener.shutDown();
        streamTrack.play();
    }

    private class StreamListener extends Thread {
        boolean listening = true;

        void shutDown() {
            listening = false;
        }

        @Override
        public void run() {
            if (listening) listen();
        }

        private void listen() {
            while (listening) {
                String stream = buffer.toString();
                if (!stream.isEmpty()) {
                    Log.d("ER", "play stream -------- " + stream);
                    byte[] data = Base64.decode(stream, Base64.DEFAULT);
                    streamTrack.write(data, 0, data.length);

                    buffer.setLength(0);
                }
            }
        }
    }

}
