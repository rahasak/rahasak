package com.score.chatz.utils;

import android.content.Context;

import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.Secret;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by eranga on 6/27/16.
 */
public class SenzUtils {
    public static Senz getPingSenz(Context context) {
        try {
            User user = PreferenceUtils.getUser(context);

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

            // new senz object
            Senz senz = new Senz();
            senz.setSenzType(SenzTypeEnum.PING);
            senz.setSender(new User("", user.getUsername()));
            senz.setReceiver(new User("", "senzswitch"));
            senz.setAttributes(senzAttributes);

            return senz;
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Senz getPubkeySenz(Context context, String user) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();

        Long timestamp = System.currentTimeMillis() / 1000;
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("uid", getUid(context, timestamp.toString()));
        senzAttributes.put("pubkey", "");
        senzAttributes.put("name", user);

        // new senz object
        Senz senz = new Senz();
        senz.setSenzType(SenzTypeEnum.GET);
        senz.setReceiver(new User("", "senzswitch"));
        senz.setAttributes(senzAttributes);

        return senz;
    }

    public static Senz getAckSenz(User user, String uid, String statusCode) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("uid", uid);
        senzAttributes.put("status", statusCode);

        // new senz object
        Senz senz = new Senz();
        senz.setSenzType(SenzTypeEnum.DATA);
        senz.setReceiver(user);
        senz.setAttributes(senzAttributes);

        return senz;
    }

    public static String getUid(Context context, String timestamp) {
        try {
            String username = PreferenceUtils.getUser(context).getUsername();
            return username + timestamp;
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    public static boolean isStreamOn(Senz senz) {
        return senz.getAttributes().containsKey("cam") && senz.getAttributes().get("cam").equalsIgnoreCase("on") ||
                senz.getAttributes().containsKey("mic") && senz.getAttributes().get("mic").equalsIgnoreCase("on");
    }

    public static boolean isStreamOff(Senz senz) {
        return senz.getAttributes().containsKey("cam") && senz.getAttributes().get("cam").equalsIgnoreCase("off") ||
                senz.getAttributes().containsKey("mic") && senz.getAttributes().get("mic").equalsIgnoreCase("off");
    }

    public static boolean isCurrentUser(String username, Context context) {
        try {
            return PreferenceUtils.getUser(context).getUsername().equalsIgnoreCase(username);
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Senz getShareSenz(Context context, String username, String sessionKey) throws NoSuchAlgorithmException {
        // create senz attributes
        Long timestamp = (System.currentTimeMillis() / 1000);
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("msg", "");
        senzAttributes.put("status", "");
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("uid", SenzUtils.getUid(context, timestamp.toString()));

        // put session key
        senzAttributes.put("$skey", sessionKey);

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.SHARE;
        User receiver = new User("", username);
        Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

        // send to service
        return senz;
    }

    public static Senz getSenzFromSecret(Secret secret) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();

        senzAttributes.put("time", secret.getTimeStamp().toString());
        senzAttributes.put("uid", secret.getTimeStamp().toString());
        if (secret.getUser().getSessionKey() != null && !secret.getUser().getSessionKey().isEmpty()) {
            senzAttributes.put("$msg", RSAUtils.encrypt(RSAUtils.getSecretKey(secret.getUser().getSessionKey()), secret.getBlob()));
        } else {
            senzAttributes.put("msg", secret.getBlob());
        }

        // new senz object
        Senz senz = new Senz();
        senz.setSenzType(SenzTypeEnum.DATA);
        senz.setReceiver(new User("", secret.getUser().getUsername()));
        senz.setAttributes(senzAttributes);

        return senz;
    }
}
