package com.score.chatz.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteConstraintException;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.SenzStream;
import com.score.chatz.services.LocationService;
import com.score.chatz.services.SenzServiceConnection;
import com.score.chatz.ui.RecordingActivity;
import com.score.chatz.utils.CameraUtils;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.SenzParser;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Handle All senz messages from here
 * <p/>
 * SENZ RECEIVERS
 * <p/>
 * 1. SENZ_SHARE
 */
public class SenzHandler {
    private static final String TAG = SenzHandler.class.getName();

    private static Context context;
    private static SenzHandler instance;
    private static SenzServiceConnection serviceConnection;
    private static SenzorsDbSource dbSource;

    private SenzStream senzStream;

    private SenzHandler() {
    }

    public static SenzHandler getInstance(Context context) {
        if (instance == null) {
            instance = new SenzHandler();
            SenzHandler.context = context.getApplicationContext();

            serviceConnection = new SenzServiceConnection(context);
            dbSource = new SenzorsDbSource(context);

            // bind to senz service
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName("com.score.chatz", "com.score.chatz.services.RemoteSenzService");
            SenzHandler.context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        return instance;
    }

    public void handleSenz(String senzMessage) {
        // parse and verify senz
        try {
            if (senzStream != null && senzStream.isActive()) {
                handleStream(senzMessage);
            } else {
                Senz senz = SenzParser.parse(senzMessage);
                verifySenz(senz);
                switch (senz.getSenzType()) {
                    case PING:
                        Log.d(TAG, "PING received");
                        break;
                    case SHARE:
                        Log.d(TAG, "SHARE received");
                        Log.d(TAG, "#lat #lon SHARE received");
                        handleShareSenz(senz);
                        break;
                    case GET:
                        Log.d(TAG, "GET received");
                        handleGetSenz(senz);
                        break;
                    case DATA:
                        Log.d(TAG, "DATA received");
                        handleDataSenz(senz);
                        break;
                }
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
    }

    private static void verifySenz(Senz senz) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        senz.getSender();

        // TODO get public key of sender
        // TODO verify signature of the senz
        //RSAUtils.verifyDigitalSignature(senz.getPayload(), senz.getSignature(), null);
    }

    /**
     * SenzUserHandler will handle all new or updates to permissions
     * @param senz
     */
    private void handleShareSenz(Senz senz){
        Log.i(TAG, "Delegating to SenzUserHandler :)");
        SenzUserHandler.getInstance().handleSenz(senz, serviceConnection.getInterface(), dbSource, context);
    }

    private void handleGetSenz(final Senz senz) {
        Log.d(TAG, senz.getSender() + " : " + senz.getSenzType().toString());

        if (senz.getAttributes().containsKey("profilezphoto") || senz.getAttributes().containsKey("chatzphoto")) {
                SenzPhotoHandler.getInstance().handleSenz(senz, serviceConnection.getInterface(), dbSource, context);

        } else if (senz.getAttributes().containsKey("chatzmic")) {
            openRecorder(senz.getSender().getUsername());
        } else if (senz.getAttributes().containsKey("lat") && senz.getAttributes().containsKey("lon")) {
            Intent serviceIntent = new Intent(context, LocationService.class);
            serviceIntent.putExtra("USER", senz.getSender());
            context.startService(serviceIntent);
        }
    }

    private void openRecorder(String sender) {
        Intent openRecordingActivity = new Intent();
        openRecordingActivity.setClass(context, RecordingActivity.class);
        openRecordingActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openRecordingActivity.putExtra("SENDER", sender);
        context.startActivity(openRecordingActivity);
    }

    private void handleDataSenz(Senz senz) {
        if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("ShareDone")) {
            /*
             * Share message from other user, to whom you send a share to.
             */
            //Add New User
            // save senz in db
            User sender = dbSource.getOrCreateUser(senz.getSender().getUsername());
            dbSource.createPermissionsForUser(senz);
            dbSource.createConfigurablePermissionsForUser(senz);
            senz.setSender(sender);
            Log.d(TAG, "save senz");
            // if senz already exists in the db, SQLiteConstraintException should throw
            try {
                dbSource.createSenz(senz);
                //NotificationUtils.showNotification(context, context.getString(R.string.new_senz), "Permission to share with @" + senz.getSender().getUsername());
            } catch (SQLiteConstraintException e) {
                Log.e(TAG, e.toString());
            }
            Intent intent = new Intent("com.score.chatz.DATA_SENZ");
            intent.putExtra("SENZ", senz);
            context.sendBroadcast(intent);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("newPerm")) {
            /*
             * New access permission you received from an a friend
             * Send permission accepted message to indicate to other user, update was succesfull
             */
            //Add New Permission
            SenzPermissionHandler.getInstance().handleSenz(senz, serviceConnection.getInterface(), dbSource, context);
            Intent intent = new Intent("com.score.chatz.DATA_SENZ");
            intent.putExtra("SENZ", senz);
            context.sendBroadcast(intent);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("sharePermDone")) {
            /*
             * New permission, set a user, is notified to be updated correctly, success message.
             */
            //Add New Permission
            if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("sharePermDone")) {
                // save senz in db
                User sender = dbSource.getOrCreateUser(senz.getSender().getUsername());
                senz.setSender(sender);
                Log.d(TAG, "save user permission");
                try {
                    dbSource.updateConfigurablePermissions(senz.getSender(), senz.getAttributes().get("camPerm"), senz.getAttributes().get("locPerm"), senz.getAttributes().get("micPerm"));
                } catch (SQLiteConstraintException e) {
                    Log.e(TAG, e.toString());
                }
                //TODO Indicate to the user if success
            } else {
                //TODO Indicate to the user if fail
            }
        } else if (senz.getAttributes().containsKey("chatzmsg")) {
            /*
             * Any new chatz message, incoming, save straight to db
             */
            //Add chat message
            SenzMessageHandler.getInstance().handleSenz(senz, serviceConnection.getInterface(), dbSource, context);
            Intent intent = new Intent("com.score.chatz.DATA_SENZ");
            intent.putExtra("SENZ", senz);
            context.sendBroadcast(intent);
        } else if (senz.getAttributes().containsKey("stream")) {
            // handle streaming
            if (senz.getAttributes().get("stream").equalsIgnoreCase("ON")) {
                Log.d(TAG, "Stream ON from " + senz.getSender().getUsername());

                senzStream = new SenzStream(true, senz.getSender().getUsername(), new StringBuilder());
                senzStream.setIsActive(true);
            }
        } else if (senz.getAttributes().containsKey("chatzphoto")) {
                // save stream to db
                try {
                    if (senz.getAttributes().containsKey("chatzphoto")) {
                        dbSource.createSecret(new Secret(null, senz.getAttributes().get("chatzphoto"), senz.getAttributes().get("chatzphoto"), senz.getSender(), senz.getReceiver()));
                    }
                } catch (SQLiteConstraintException e) {
                    Log.e(TAG, e.toString());
                }

                // broadcast
                Intent intent = new Intent("com.score.chatz.DATA_SENZ");
                intent.putExtra("SENZ", senz);
                context.sendBroadcast(intent);

        } else if (senz.getAttributes().containsKey("profilezphoto")) {
            // save stream to db
            try {
                if (senz.getAttributes().containsKey("profilezphoto")) {
                    User sender = senz.getSender();
                    dbSource.insertImageToDB(sender.getUsername(), senz.getAttributes().get("profilezphoto"));
                }
            } catch (SQLiteConstraintException e) {
                Log.e(TAG, e.toString());
            }

            // broadcast
            Intent intent = new Intent("com.score.chatz.DATA_SENZ");
            intent.putExtra("SENZ", senz);
            context.sendBroadcast(intent);

        }  else if (senz.getAttributes().containsKey("chatzsound")) {
            // save stream to db
            try {
                if (senz.getAttributes().containsKey("chatzsound")) {
                    User sender = senz.getSender();
                    Secret secret = new Secret(null, null, null, senz.getReceiver(), senz.getSender());
                    secret.setSound(senz.getAttributes().get("chatzsound"));
                    dbSource.createSecret(secret);
                }
            } catch (SQLiteConstraintException e) {
                Log.e(TAG, e.toString());
            }

            // broadcast
            Intent intent = new Intent("com.score.chatz.DATA_SENZ");
            intent.putExtra("SENZ", senz);
            context.sendBroadcast(intent);

        } else {
            /*
             * Default cases, handle such as registration success or, any other scenarios where need to specifically handle in the Activity.
             */
            Intent intent = new Intent("com.score.chatz.DATA_SENZ");
            intent.putExtra("SENZ", senz);
            context.sendBroadcast(intent);
        }

        /*
         * The following method is used to notify all list view to update.
         */
        handleDataChanges(senz);

    }

    private void handleStream(String stream) {
        if (senzStream != null && senzStream.isActive()) {
            if (stream.contains("#stream off")) {
                Log.d(TAG, "Stream OFFFFFFFFFFFF ");
                senzStream.setIsActive(false);
                //Stream has ended. Already receieved Stream oFF.. we need to notify the handleSenz method to process the stream
                handleSenz(senzStream.getSenzString());

            }else{
                // streaming ON
                setStreamType(stream);
                Log.d(TAG, "Stream ON, chatzphoto Data SAVED : " + stream);
                senzStream.putStream(stream);
            }
        } else {
            Log.e(TAG, "Stream OFF, chatzphoto ");
        }
    }

    /**
     * set the type of stream if not already done.
     * @param stream
     */
    private void setStreamType(String stream){
        if(senzStream.getStreamType() == null){
            if (stream.contains("#chatzphoto")) {
                senzStream.setStreamType(SenzStream.SENZ_STEAM_TYPE.CHATZPHOTO);
            }else if (stream.contains("#profilezphoto")){
                senzStream.setStreamType(SenzStream.SENZ_STEAM_TYPE.PROFILEZPHOTO);
            }else if (stream.contains("#chatzsound")){
                senzStream.setStreamType(SenzStream.SENZ_STEAM_TYPE.CHATZSOUND);
            }
        }
    }


    private void handleDataChanges(Senz senz) {
        Intent intent = new Intent("com.score.chatz.USER_UPDATE");
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    public SenzServiceConnection getServiceConnection(){
        return serviceConnection;
    }
}

