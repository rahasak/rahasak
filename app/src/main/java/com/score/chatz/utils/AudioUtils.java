package com.score.chatz.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by Lakmal on 8/28/16.
 */
public class AudioUtils {

    public static final int RECORDER_SAMPLE_RATE = 8000;

    public static void F(final byte[] mp3SoundByteArray, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // first disable speaker
                disableSpeaker(context);

                int bufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                AudioTrack mAudioTrack = new AudioTrack(AudioManager.MODE_IN_CALL, RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

                mAudioTrack.play();
                mAudioTrack.write(mp3SoundByteArray, 0, mp3SoundByteArray.length);
                mAudioTrack.stop();

                // first disable speaker
                enableSpeaker(context);
            }
        }).start();
    }

    public static void disableSpeaker(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(false);
    }

    public static void enableSpeaker(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);
    }

    public static void resetAudioManager(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }
}
