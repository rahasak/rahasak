package com.score.chatz.handlers;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.CameraBusyException;
import com.score.chatz.interfaces.IDataPhotoSenzHandler;
import com.score.chatz.interfaces.IReceivingComHandler;
import com.score.chatz.interfaces.ISendAckHandler;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.SenzStream;
import com.score.chatz.services.SenzServiceConnection;
import com.score.chatz.utils.CameraUtils;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class SenzPhotoHandler extends BaseHandler implements ISendAckHandler, IDataPhotoSenzHandler, IReceivingComHandler {
    private static final String TAG = SenzPhotoHandler.class.getName();
    private static SenzPhotoHandler instance;

    /**
     * Singleton
     *
     * @return
     */
    public static SenzPhotoHandler getInstance() {
        if (instance == null) {
            instance = new SenzPhotoHandler();
        }
        return instance;
    }

    /**
     * Share back to user if unsuccessful
     *
     * @param senzService
     * @param receiver
     * @param isDone
     */
    public void sendConfirmation(Senz _senz, ISenzService senzService, User receiver, boolean isDone) {
        if (isDone == false) {
            try {
                // create senz attributes
                HashMap<String, String> senzAttributes = new HashMap<>();
                senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
                senzAttributes.put("msg", "SensorNotAvailable");

                String id = "_ID";
                String signature = "";
                SenzTypeEnum senzType = SenzTypeEnum.DATA;
                Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

                senzService.send(senz);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initiate send photo from here!!
     *
     * @param image
     * @param senz
     * @param context
     */
    public void sendPhoto(final byte[] image, final Senz senz, final Context context) {

        //Get servicve connection
        final SenzServiceConnection serviceConnection = SenzHandler.getInstance(context).getServiceConnection();

        serviceConnection.executeAfterServiceConnected(new Runnable() {
            @Override
            public void run() {
                // service instance
                ISenzService senzService = serviceConnection.getInterface();

                // service instance
                Log.d(TAG, "send response(share back) for photo : " + image);

                Log.i(TAG, "USER INFO - senz.getSender() : " + senz.getSender().getUsername() + ", senz.getReceiver() : " + senz.getReceiver().getUsername());
                try {
                    // compose senzes
                    String uid = SenzUtils.getUniqueRandomNumber().toString();

                    Senz startSenz = getStartPhotoSharingSenze(senz);
                    senzService.send(startSenz);


                    String uriToFindImageForService = SenzUtils.getUniqueRandomNumber().toString();
                    CameraUtils.savePhotoCache(uriToFindImageForService, CameraUtils.getBitmapFromBytes(Base64.encode(image, 0)), context);


                     /*ArrayList<Senz> photoSenzList = getPhotoStreamingSenz(senz, image, context, uid);

                    Senz stopSenz = getStopPhotoSharingSenz(senz);

                    ArrayList<Senz> senzList = new ArrayList<Senz>();
                    senzList.addAll(photoSenzList);
                    senzList.add(stopSenz);

                    //senzService.sendInOrder(senzList);*/
                    senzService.sendFromUri(uriToFindImageForService, senz, uid);


                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private ArrayList<Senz> getPhotoStreamingSenz(Senz senz, byte[] image, Context context, String uid) {
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


    private Senz getStartPhotoSharingSenze(Senz senz) {
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

    private Senz getStopPhotoSharingSenz(Senz senz) {
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

    /**
     * Launch your camera from here!!
     *
     * @param context
     * @param senz
     * @throws CameraBusyException
     */
    public void launchCamera(Context context, Senz senz) throws CameraBusyException {
        //Start
        Intent intent = IntentProvider.getCameraIntent(context);

        //To pass:
        intent.putExtra("Senz", senz);
        if (senz.getAttributes().containsKey("chatzphoto")) {
            intent.putExtra("StreamType", SenzStream.SENZ_STEAM_TYPE.CHATZPHOTO);
        } else if (senz.getAttributes().containsKey("profilezphoto")) {
            intent.putExtra("StreamType", SenzStream.SENZ_STEAM_TYPE.PROFILEZPHOTO);
        }

        try {
            context.startActivity(intent);
        } catch (Exception ex) {
            Log.d(TAG, "Camera might already be in use... exception: " + ex);
            throw new CameraBusyException();
        }
    }

    public static void sendPhotoRecievedConfirmation(final Senz senz, final Context context, final String uid, final Boolean isDone) {
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
                        senzAttributes.put("msg", "photoSent");
                    } else {
                        senzAttributes.put("msg", "photoSentFail");
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

    @Override
    public void onPhotoSent(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        dbSource.markSecretDelievered(senz.getAttributes().get("uid"));
        broadcastDataSenz(senz, context);
    }

    @Override
    public void onNewChatPhoto(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        try {
            if (senz.getAttributes().containsKey("chatzphoto")) {
                //Secret newSecret = new Secret(null, senz.getAttributes().get("chatzphoto"), senz.getAttributes().get("chatzphoto"), senz.getSender(), senz.getReceiver());
                Secret newSecret = new Secret(senz.getAttributes().get("chatzphoto"), "IMAGE", senz.getSender());
                newSecret.setReceiver(senz.getReceiver());

                String uid = senz.getAttributes().get("uid");
                newSecret.setID(uid);
                Long _timeStamp = System.currentTimeMillis();
                newSecret.setTimeStamp(_timeStamp);
                dbSource.createSecret(newSecret);
                sendPhotoRecievedConfirmation(senz, context, uid, true);

                broadcastDataSenz(senz, context);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onNewProfilePhoto(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        try {
            if (senz.getAttributes().containsKey("profilezphoto")) {
                User sender = senz.getSender();
                if (sender != null)
                    dbSource.insertImageToDB(sender.getUsername(), senz.getAttributes().get("profilezphoto"));

                broadcastDataSenz(senz, context);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void handleGetSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        //If camera not available send unsuccess confirmation to user
        if (CameraUtils.isCameraFrontAvailable(context)) {
            //Start camera activity
            try {
                launchCamera(context, senz);
            } catch (CameraBusyException ex) {
                Log.e(TAG, "Camera is busy right now.");
                sendConfirmation(null, senzService, senz.getSender(), false);
            }
        } else {
            //Camera not available on this phone
            sendConfirmation(null, senzService, senz.getSender(), false);
        }
    }

    @Override
    public void handleShareSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        //Nothing to share!!!
    }
}
