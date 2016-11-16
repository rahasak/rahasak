package com.score.chatz.utils;

import android.content.Context;

import com.score.chatz.exceptions.NoUserException;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

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

    public static String getPubkeySenz(Context context, User receiver) {
        try {
            PrivateKey privateKey = RSAUtils.getPrivateKey(context);
            User user = PreferenceUtils.getUser(context);

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("pubkey", "pubkey");

            // new senz object
            Senz senz = new Senz();
            senz.setSenzType(SenzTypeEnum.GET);
            senz.setSender(new User("", user.getUsername()));
            senz.setReceiver(receiver);
            senz.setAttributes(senzAttributes);

            // get digital signature of the senz
            String senzPayload = SenzParser.getSenzPayload(senz);
            String senzSignature = RSAUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);

            return SenzParser.getSenzMessage(senzPayload, senzSignature);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoUserException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
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

}
