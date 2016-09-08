package com.score.chatz.handlers;

import android.content.Context;
import android.os.RemoteException;

import com.score.chatz.pojo.Secret;
import com.score.chatz.services.SenzServiceConnection;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class SenzSoundHandler extends BaseHandler {
    private static final String TAG = SenzSoundHandler.class.getName();
    private static SenzSoundHandler instance;

    /**
     * Singleton
     *
     * @return
     */
    public static SenzSoundHandler getInstance() {
        if (instance == null) {
            instance = new SenzSoundHandler();
        }
        return instance;
    }

    public void sendSound(final Secret secret, Context context) {

        //Get servicve connection
        final SenzServiceConnection serviceConnection = SenzHandler.getInstance(context).getServiceConnection();

        serviceConnection.executeAfterServiceConnected(new Runnable() {
            @Override
            public void run() {
                // service instance
                ISenzService senzService = serviceConnection.getInterface();

                try {
                    // compose senzes
                    Senz startSenz = getStartSoundSharingSenze(secret);
                    senzService.send(startSenz);

                    ArrayList<Senz> photoSenzList = getSoundStreamingSenz(secret);
                    //Senz photoSenz = getPhotoSenz(senz, image);
                    //senzService.send(photoSenz);

                    Senz stopSenz = getStopSoundSharingSenz(secret);
                    //senzService.send(stopSenz);

                    ArrayList<Senz> senzList = new ArrayList<Senz>();
                    senzList.addAll(photoSenzList);
                    senzList.add(stopSenz);
                    senzService.sendInOrder(senzList);


                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private ArrayList<Senz> getSoundStreamingSenz(Secret secret) {
        String soundString = secret.getSound();
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


            Senz _senz = new Senz(id, signature, senzType, secret.getReceiver(), secret.getSender(), senzAttributes);
            senzList.add(_senz);
        }

        return senzList;
    }

    private Senz getStartSoundSharingSenze(Secret secret) {
        //senz is the original senz
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("stream", "on");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.DATA;
        Senz _senz = new Senz(id, signature, senzType, secret.getReceiver(), secret.getSender(), senzAttributes);
        return _senz;
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
        Senz _senz = new Senz(id, signature, senzType, secret.getReceiver(), secret.getSender(), senzAttributes);
        return _senz;
    }
}
