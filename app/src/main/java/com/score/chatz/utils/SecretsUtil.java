package com.score.chatz.utils;

import android.util.Log;

import com.score.chatz.pojo.Secret;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Created by lakmal.caldera on 9/10/2016.
 */
public class SecretsUtil {

    private static final String TAG = SecretsUtil.class.getName();

    //Time to display message in chatview in minutes
    private static final int LENGTH_OF_TIME_TO_DISPLAY_SECRET = 1;

    public static boolean isSecretToBeShown(Secret secret){
        boolean showSecret;
        if(secret.getSeenTimeStamp() != null) {
            Timestamp timestamp = new Timestamp(secret.getSeenTimeStamp());
            Date date = new Date(timestamp.getTime());
            if (TimeUtils.isDatePast(date, LENGTH_OF_TIME_TO_DISPLAY_SECRET)) {
                showSecret = false;
            } else {
                showSecret = true;
            }
        }else{
            Log.i("SecretUtil", "TIMESTAMP SEEN NOT SET: NULL " + secret.getID());
            showSecret = true;
        }
        return showSecret;
    }
}
