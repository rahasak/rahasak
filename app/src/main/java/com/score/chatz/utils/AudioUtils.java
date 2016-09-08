package com.score.chatz.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by Lakmal on 8/28/16.
 */
public class AudioUtils {

    public static final int RECORDER_SAMPLERATE = 8000;

    public static void play(byte[] mp3SoundByteArray, Context context)
    {
        disableSpeaker(context);

        int bufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        mAudioTrack.play();

        mAudioTrack.write(mp3SoundByteArray, 0, mp3SoundByteArray.length);

        enableSpeaker(context);

    }

    public static void disableSpeaker(Context context){
        AudioManager audioManager = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.STREAM_MUSIC);
        audioManager.setSpeakerphoneOn(false);
    }

    public static void enableSpeaker(Context context){
        AudioManager audioManager = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);
    }
}
