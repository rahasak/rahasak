package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.utils.AudioUtils;

public class StreamPlayer {

    private Context context;
    private StringBuffer buffer;
    private Player player;

    public StreamPlayer(Context context) {
        this.context = context;
        buffer = new StringBuffer();
        player = new Player();
    }

    public void play() {
        player.start();
    }

    public void onStream(String stream) {
        buffer.append(stream);
    }

    public void stop() {
        player.shutDown();
    }

    private class Player extends Thread {
        private AudioTrack streamTrack;
        private boolean playing = true;

        @Override
        public void run() {
            if (playing) {
                startPlay();
                play();
            }
        }

        private void startPlay() {
            int minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    AudioUtils.RECORDER_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                    AudioTrack.MODE_STREAM);

            enableEarpiece();
            streamTrack.play();
        }

        private void play() {
            while (playing) {
                String stream = buffer.toString();
                if (!stream.isEmpty()) {
                    Log.d("PLAY", stream.length() + "------------");
                    byte[] data = Base64.decode(stream, Base64.DEFAULT);
                    streamTrack.write(data, 0, data.length);
                    buffer.setLength(0);
                }
            }
        }

        void shutDown() {
            playing = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                streamTrack.pause();
                streamTrack.flush();
            } else {
                streamTrack.stop();
            }
        }
    }

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.STREAM_VOICE_CALL);
        audioManager.setSpeakerphoneOn(false);
    }

}
