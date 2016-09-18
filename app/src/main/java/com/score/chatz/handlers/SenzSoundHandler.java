package com.score.chatz.handlers;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.RemoteException;
import android.util.Log;

import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.interfaces.IDataSoundSenzHandler;
import com.score.chatz.interfaces.IReceivingComHandler;
import com.score.chatz.interfaces.ISendAckHandler;
import com.score.chatz.pojo.Secret;
import com.score.chatz.services.SenzServiceConnection;
import com.score.chatz.ui.RecordingActivity;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class SenzSoundHandler extends BaseHandler implements IReceivingComHandler, IDataSoundSenzHandler {
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

    @Override
    public void handleGetSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        if(RecordingActivity.isAcitivityActive() == false) {
            openRecorder(senz.getSender().getUsername(), context);
        }else{
            SenzSoundHandler.sendBusyNotification(senz, context);
        }
    }

    @Override
    public void handleShareSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {

    }

    public void sendSound(final Secret secret, final Context context, final String uid) {

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

                    ArrayList<Senz> photoSenzList = getSoundStreamingSenz(secret, context, uid);
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
        Senz _senz = new Senz(id, signature, senzType, secret.getWho(), secret.getReceiver(), senzAttributes);
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
        Senz _senz = new Senz(id, signature, senzType, secret.getWho(), secret.getReceiver(), senzAttributes);
        return _senz;
    }

    private static void sendSoundRecievedConfirmation(final Senz senz, final Context context, final String uid, final Boolean isDone){
        //Get servicve connection
        final SenzServiceConnection serviceConnection = SenzHandler.getInstance(context).getServiceConnection();

        serviceConnection.executeAfterServiceConnected(new Runnable() {
            @Override
            public void run() {
                // service instance
                ISenzService senzService = serviceConnection.getInterface();
                try {
                    // create senz attributes
                    HashMap<String, String> senzAttributes = new HashMap<>();
                    senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

                    //This is unique identifier for each message
                    senzAttributes.put("uid", uid);
                    if (isDone) {
                        senzAttributes.put("msg", "soundSent");
                    } else {
                        senzAttributes.put("msg", "soundSentFail");
                    }
                    // new senz
                    String id = "_ID";
                    String signature = "_SIGNATURE";
                    SenzTypeEnum senzType = SenzTypeEnum.DATA;
                    Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);

                    senzService.send(_senz);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void openRecorder(String sender, Context context) {
        Intent openRecordingActivity = new Intent();
        openRecordingActivity.setClass(context, RecordingActivity.class);
        openRecordingActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openRecordingActivity.putExtra("SENDER", sender);
        context.startActivity(openRecordingActivity);
    }


    @Override
    public void onSoundSent(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        dbSource.markSecretDelievered(senz.getAttributes().get("uid"));
        broadcastDataSenz(senz, context);
    }

    @Override
    public void onNewChatSound(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        try {
            if (senz.getAttributes().containsKey("chatzsound")) {
                User sender = senz.getSender();
                //Secret secret = new Secret(null, null, null, senz.getSender(), senz.getReceiver());
                Secret secret = new Secret(senz.getAttributes().get("chatzsound"), "SOUND", senz.getSender());
                secret.setReceiver(senz.getReceiver());
                Long _timeStamp = System.currentTimeMillis();
                secret.setTimeStamp(_timeStamp);

                String uid = senz.getAttributes().get("uid");
                secret.setID(uid);

                dbSource.createSecret(secret);
                SenzSoundHandler.sendSoundRecievedConfirmation(senz, context, uid, true);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, e.toString());
        }
    }
}
