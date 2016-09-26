package com.score.chatz.asyncTasks;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

import com.score.chatz.utils.AudioUtils;

/**
 * Created by eranga on 9/18/16.
 */
public class RahasPlayer extends AsyncTask<String, String, String> {

    private byte[] rahasa;
    private Context context;
    private Activity activity;

    public RahasPlayer(byte[] rahasa, Context context, Activity activity) {
        this.rahasa = rahasa;
        this.context = context;
        this.activity = activity;
    }

    public RahasPlayer(byte[] rahasa, Context context) {
        this.rahasa = rahasa;
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        // first disable speaker
        AudioUtils.disableSpeaker(context);

        int bufferSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack rahasaTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);

        // play rahasa
        rahasaTrack.play();
        rahasaTrack.write(rahasa, 0, rahasa.length);
        rahasaTrack.stop();

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if(activity != null){
            activity.finish();
        }
    }
}

