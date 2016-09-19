package com.score.chatz.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by lakmal.caldera on 9/17/2016.
 */
public class RemoteServiceUtils {
    /*private static final String TAG = RemoteServiceUtils.class.getName();

    private static String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }

    public static ArrayList<Senz> getPhotoStreamingSenz(Senz senz, byte[] image, Context context, String uid) {
        String imageAsString = Base64.encodeToString(image, Base64.DEFAULT);
        String thumbnail = CameraUtils.resizeBase64Image(imageAsString);

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] imgs = split(imageAsString, 1024);

        if (senz.getAttributes().containsKey("chatzphoto")) {
            //Save photo to db before sending if its a chatzphoto
            //Secret newSecret = new Secret(null, imageAsString, thumbnail, senz.getReceiver(), senz.getSender());
            Secret newSecret = new Secret(imageAsString, "IMAGE", senz.getReceiver());
            newSecret.setReceiver(senz.getReceiver());
            Long _timeStamp = System.currentTimeMillis();
            newSecret.setTimeStamp(_timeStamp);
            newSecret.setID(uid);
            new SenzorsDbSource(context).createSecret(newSecret);
        }

        for (int i = 0; i < imgs.length; i++) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            if (senz.getAttributes().containsKey("chatzphoto")) {
                senzAttributes.put("chatzphoto", imgs[i].trim());
            } else if (senz.getAttributes().containsKey("profilezphoto")) {
                senzAttributes.put("profilezphoto", imgs[i].trim());
            }

            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
            senzList.add(_senz);
        }

        return senzList;
    }


    public static Senz getStartPhotoSharingSenze(Senz senz) {
        //senz is the original senz
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("stream", "on");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Log.i(TAG, "Senz receiver - " + senz.getReceiver());
        Log.i(TAG, "Senz sender - " + senz.getSender());
        Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
        return _senz;
    }

    public static Senz getStopPhotoSharingSenz(Senz senz) {
        // create senz attributes
        //senz is the original senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("stream", "off");


        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
        return _senz;
    }

    private ArrayList<Senz> getSoundStreamingSenz(Secret secret, Context context, String uid) {
        String soundString = secret.getBlob();
        //secret.setID(uid);
        //new SenzorsDbSource(context).createSecret(secret);

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] imgs = split(soundString, 1024);
        for (int i = 0; i < imgs.length; i++) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("chatzsound", imgs[i].trim());
            senzAttributes.put("uid", uid);


            Senz _senz = new Senz(id, signature, senzType, secret.getWho(), secret.getReceiver() , senzAttributes);
            senzList.add(_senz);
        }

        return senzList;
    }

    private Senz getStopSoundSharingSenz(Secret secret) {
        // create senz attributes
        //senz is the original senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("stream", "off");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, secret.getWho(), secret.getReceiver(), senzAttributes);
        return _senz;
    }*/
}
