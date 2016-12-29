package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.SenzBuffer;

public class StreamPlayer {

    private Context context;
    private SenzBuffer senzBuffer;
    private Player player;

    int minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

    public StreamPlayer(Context context) {
        this.context = context;
        senzBuffer = new SenzBuffer();
        player = new Player();
    }

    public void play() {
        player.start();
    }

    public void onStream(byte[] stream) {
        senzBuffer.put(stream);
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
                if (senzBuffer.size() > minBufSize) {
                    byte[] data = senzBuffer.get(0, minBufSize);

                    streamTrack.write(data, 0, data.length);
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
