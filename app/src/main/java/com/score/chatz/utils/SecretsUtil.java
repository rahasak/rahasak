package com.score.chatz.utils;

import com.score.chatz.pojo.Secret;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by lakmal.caldera on 9/10/2016.
 */
public class SecretsUtil {

    private static final String TAG = SecretsUtil.class.getName();

    //Time to display message in chatview in minutes
    private static final int LENGTH_OF_TIME_TO_DISPLAY_SECRET = 5;

    public static boolean isSecretToBeShown(Secret secret){
        boolean showSecret;
        Timestamp timestamp = new Timestamp(secret.getTimeStamp());
        Date date = new Date(timestamp.getTime());
        if(TimeUtils.isDatePast(date, LENGTH_OF_TIME_TO_DISPLAY_SECRET)){
            showSecret = false;
        }else{
            showSecret = true;
        }
        return showSecret;
    }
}
