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
import com.score.chatz.utils.SecretsUtil;
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
        if (!isDone) {
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
        // service connection
        final SenzServiceConnection serviceConnection = SenzHandler.getInstance(context).getServiceConnection();

        serviceConnection.executeAfterServiceConnected(new Runnable() {
            @Override
            public void run() {
                // service instance
                ISenzService senzService = serviceConnection.getInterface();

                Log.d(TAG, "send response(share back) for photo : " + image);
                Log.i(TAG, "user info, sender : " + senz.getSender().getUsername() + ", receiver : " + senz.getReceiver().getUsername());

                try {
                    // compose senz
                    String uid = SenzUtils.getUniqueRandomNumber();

                    // stream on senz
                    // stream content
                    // stream off senz
                    Senz startStreamSenz = getStartStreamSenz(senz);
                    ArrayList<Senz> photoSenzList = getPhotoStreamSenz(senz, image, context, uid);
                    Senz stopStreamSenz = getStopStreamSenz(senz);

                    // populate list
                    ArrayList<Senz> senzList = new ArrayList<>();
                    senzList.add(startStreamSenz);
                    senzList.addAll(photoSenzList);
                    senzList.add(stopStreamSenz);

                    senzService.sendInOrder(senzList);

                    // TODO save photo in cache and send the uri to the service
                    //String uriToFindImageForService = SenzUtils.getUniqueRandomNumber();
                    //CameraUtils.savePhotoCache(uriToFindImageForService, CameraUtils.getBitmapFromBytes(Base64.encode(image, 0)), context);
                    //senzService.sendFromUri(uriToFindImageForService, senz, uid);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Decompose image stream in to multiple data/stream senz's
     *
     * @param senz    original senz
     * @param image   image content
     * @param context app context
     * @param uid     unique id
     * @return list of decomposed senz's
     */
    private ArrayList<Senz> getPhotoStreamSenz(Senz senz, byte[] image, Context context, String uid) {
        String imageString = Base64.encodeToString(image, Base64.DEFAULT);

        // save photo to db before sending if its a chatzphoto
        if (senz.getAttributes().containsKey("chatzphoto")) {
            Secret newSecret = new Secret(imageString, "IMAGE", senz.getReceiver(), false);
            Long timeStamp = System.currentTimeMillis();
            newSecret.setTimeStamp(timeStamp);
            newSecret.setID(uid);
            new SenzorsDbSource(context).createSecret(newSecret);
        }

        ArrayList<Senz> senzList = new ArrayList<>();
        String[] packets = split(imageString, 1024);

        for (String packet : packets) {
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.STREAM;

            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            if (senz.getAttributes().containsKey("chatzphoto")) {
                senzAttributes.put("chatzphoto", packet.trim());
            } else if (senz.getAttributes().containsKey("profilezphoto")) {
                senzAttributes.put("profilezphoto", packet.trim());
            }

            senzAttributes.put("uid", uid);

            Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
            senzList.add(_senz);
        }

        return senzList;
    }

    /**
     * Create start stream senz
     *
     * @param senz original senz
     * @return senz
     */
    private Senz getStartStreamSenz(Senz senz) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("stream", "on");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        return new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
    }

    /**
     * Create stop stream senz
     *
     * @param senz original senz
     * @return senz
     */
    private Senz getStopStreamSenz(Senz senz) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put("stream", "off");

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.STREAM;

        return new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);
    }

    /**
     * Launch your camera from here!!
     *
     * @param context app context
     * @param senz    senz
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
    public void onNewChatPhoto(Senz senz, ISenzService senzService, final SenzorsDbSource dbSource, Context context) {
        try {
            if (senz.getAttributes().containsKey("chatzphoto")) {
                User user = SecretsUtil.getTheUser(senz.getSender(), senz.getReceiver(), context);
                final Secret newSecret = new Secret(senz.getAttributes().get("chatzphoto"), "IMAGE", user, SecretsUtil.isThisTheUsersSecret(user, senz.getSender()));
                newSecret.setReceiver(senz.getReceiver());

                String uid = senz.getAttributes().get("uid");
                newSecret.setID(uid);
                Long _timeStamp = System.currentTimeMillis();
                newSecret.setTimeStamp(_timeStamp);


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //dbSource.deleteAllSecretsThatBelongToUser(newSecret.getUser());
                        dbSource.createSecret(newSecret);
                    }
                }).start();


                sendPhotoRecievedConfirmation(senz, context, uid, true);

                broadcastDataSenz(senz, context);

                // Notify if acitvity to display new data!! fullscreen or playaback!!
                broadcastNewDataToDisplaySenz(senz, context);

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
