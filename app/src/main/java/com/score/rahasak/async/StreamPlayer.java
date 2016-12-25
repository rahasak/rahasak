package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Base64;

import com.score.rahasak.utils.AudioUtils;

public class StreamPlayer {

    private Context context;
    private StringBuffer buffer;
    private StreamListener listener;

    private AudioTrack streamTrack;

    public StreamPlayer(Context context) {
        this.context = context;
        buffer = new StringBuffer();
        listener = new StreamListener();

        int minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                AudioUtils.RECORDER_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize,
                AudioTrack.MODE_STREAM);
    }

    public void play() {
        listener.start();
        enableEarpiece();
        streamTrack.play();
    }

    public void onStream(String stream) {
        buffer.append(stream);
    }

    public void stop() {
        listener.shutDown();
        streamTrack.stop();
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
                    byte[] data = Base64.decode(stream, Base64.DEFAULT);
                    streamTrack.write(data, 0, data.length);

                    buffer.setLength(0);
                }
            }
        }
    }

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.STREAM_VOICE_CALL);
        audioManager.setSpeakerphoneOn(false);
    }

}
