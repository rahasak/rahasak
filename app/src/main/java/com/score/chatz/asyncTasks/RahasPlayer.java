package com.score.chatz.asyncTasks;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import com.score.chatz.interfaces.IRahasPlayListener;
import com.score.chatz.utils.AudioUtils;

/**
 * Created by eranga on 9/18/16.
 */
public class RahasPlayer extends AsyncTask<String, String, String> {

    private final static String TAG = RahasPlayer.class.getName();

    private byte[] rahasa;
    private Context context;
    private IRahasPlayListener listener;

    public RahasPlayer(byte[] rahasa, Context context, IRahasPlayListener listener) {
        this.rahasa = rahasa;
        this.context = context;
        this.listener = listener;

        Log.d(TAG, "init player");
    }

    @Override
    protected String doInBackground(String... params) {
        Log.d(TAG, "playing secret");

        int bufferSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack rahasaTrack = new AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                AudioUtils.RECORDER_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        // play via earpiece
        enableEarpiece();

        // play rahasa
        rahasaTrack.play();
        rahasaTrack.write(rahasa, 0, rahasa.length);
        rahasaTrack.stop();

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        Log.d(TAG, "finish playing secret");

        super.onPostExecute(s);
        listener.onFinishPlay();
    }

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.STREAM_VOICE_CALL);
        audioManager.setSpeakerphoneOn(false);
    }
}

