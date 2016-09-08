package com.score.chatz.handlers;

import android.content.Context;
import android.content.Intent;

import com.score.chatz.db.SenzorsDbSource;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

/**
 * Created by Lakmal on 9/4/16.
 */
public class BaseHandler {
    private static final String TAG = BaseHandler.class.getName();

    protected static void broadcastDataSenz(Senz senz, Context context){
        Intent intent = AppIntentHandler.getDataSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    protected static void broadcastUpdateSenz(Senz senz, Context context){
        Intent intent = AppIntentHandler.getUpdateSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    protected String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }

}
