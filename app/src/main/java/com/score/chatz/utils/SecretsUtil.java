package com.score.chatz.utils;

import android.content.Context;
import android.util.Log;

import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.Secret;
import com.score.senzc.pojos.User;

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

    public static User getTheUser(User sender, User receiver, Context context){
        User user = null;
        User currentUser = getCurrentUser(context);
        if(!sender.getUsername().equalsIgnoreCase(currentUser.getUsername())){
            user = sender;
        }else if(!receiver.getUsername().equalsIgnoreCase(currentUser.getUsername())){
            user = receiver;
        }
        return user;
    }

    public static Boolean isThisTheUsersSecret(User user, User sender){
        Boolean isMine = false;
        if(sender.getUsername().equalsIgnoreCase(user.getUsername())){
            isMine = true;
        }
        return isMine;
    }

    public static User getCurrentUser(Context context){
        User currentUser = null;
        try{
            currentUser = PreferenceUtils.getUser(context);
        }catch(NoUserException ex){
            ex.printStackTrace();
        }
        return currentUser;
    }
}
