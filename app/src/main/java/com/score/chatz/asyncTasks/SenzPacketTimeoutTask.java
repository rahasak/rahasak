package com.score.chatz.asyncTasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import com.score.chatz.handlers.AppIntentHandler;
import com.score.chatz.handlers.SenzStatusTracker;
import com.score.senzc.pojos.Senz;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by Lakmal on 9/12/16.
*/
public class SenzPacketTimeoutTask extends AsyncTask<Void, Void, Void> {
    private Integer timeout;
    private Boolean killerFlag;
    private Senz senzSend;
    private Context context;

    public SenzPacketTimeoutTask(Integer to, Senz senz, Context con) {
        timeout = to;
        killerFlag = true;
        senzSend = senz;
        context = con;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    @Override
    protected Void doInBackground(Void... params) {
        while (killerFlag && !isCancelled()){
            timeout--;
            try {
                Thread.sleep(1000);
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
            if(timeout == 0){
                killerFlag = false;
            }
        }
        if(!killerFlag) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    SenzStatusTracker.onTmeout(senzSend, context);
                }
            });
        }

        return null;
    }
}
