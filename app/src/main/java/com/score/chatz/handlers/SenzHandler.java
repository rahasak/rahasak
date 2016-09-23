package com.score.chatz.handlers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.score.chatz.application.SenzStatusTracker;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.SenzStream;
import com.score.chatz.pojo.Stream;
import com.score.chatz.services.LocationService;
import com.score.chatz.services.SenzServiceConnection;
import com.score.chatz.utils.SenzParser;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;


/**
 * Handle All senz messages from here
 * <p/>
 * SENZ RECEIVERS
 * <p/>
 * 1. SENZ_SHARE
 */
public class SenzHandler extends BaseHandler {
    private static final String TAG = SenzHandler.class.getName();

    private static Context context;
    private static SenzHandler instance;
    private static SenzServiceConnection serviceConnection;
    private static SenzorsDbSource dbSource;

    private SenzStream senzStream;
    private Stream stream;

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
                senz.setId(SenzUtils.getUniqueRandomNumber());
                verifySenz(senz);
                if (senz.getSenzType() != SenzTypeEnum.STREAM)
                    SenzStatusTracker.getInstance(context).stopSenzTrack(senz);
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
                    case STREAM:
                        Log.d(TAG, "STREAM received");
                        handleStreamSenz(senz);
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
     *
     * @param senz
     */
    private void handleShareSenz(Senz senz) {
        Log.i(TAG, "Delegating to SenzUserHandler :)");
        SenzUserHandler.getInstance().handleShareSenz(senz, serviceConnection.getInterface(), dbSource, context);
    }

    private void handleGetSenz(final Senz senz) {
        Log.d(TAG, senz.getSender() + " : " + senz.getSenzType().toString());
        if (senz.getAttributes().containsKey("profilezphoto") || senz.getAttributes().containsKey("chatzphoto")) {
            SenzPhotoHandler.getInstance().handleGetSenz(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("chatzmic")) {
            SenzSoundHandler.getInstance().handleGetSenz(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("lat") && senz.getAttributes().containsKey("lon")) {
            Intent serviceIntent = new Intent(context, LocationService.class);
            serviceIntent.putExtra("USER", senz.getSender());
            context.startService(serviceIntent);
        }
    }

    private void handleDataSenz(Senz senz) {
        if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("ShareDone")) {
            // User Sharing done!!!!
            SenzUserHandler.getInstance().onShareDone(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("newPerm")) {
            // New Permission!!!!!
            SenzPermissionHandler.getInstance().onNewPermission(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("sharePermDone")) {
            // Permission Sharing done!!!!!
            SenzPermissionHandler.getInstance().onPermissionSharingDone(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("MsgSent")) {
            // Chat Message Sent Ack!!!!!
            SenzMessageHandler.getInstance().onMessageSent(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("photoSent")) {
            // Chat Photo Sent Ack!!!!!
            SenzPhotoHandler.getInstance().onPhotoSent(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("soundSent")) {
            // Sound Sent Ack!!!!!!
            SenzSoundHandler.getInstance().onSoundSent(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("userBusy")) {
            // Broadcast User don't want to respond to your annoying sensor requests!!!!!
            broadcastUserBusySenz(senz, context);
        } else if (senz.getAttributes().containsKey("chatzmsg")) {
            // New Chat message rec!!!!!!!
            SenzMessageHandler.getInstance().onNewMessage(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("stream")) {
            // handle streaming
            if (senz.getAttributes().get("stream").equalsIgnoreCase("ON")) {
                Log.d(TAG, "Stream ON from " + senz.getSender().getUsername());
                senzStream = new SenzStream(true, senz.getSender().getUsername(), new StringBuilder());
                senzStream.setIsActive(true);
            }
        } else if (senz.getAttributes().containsKey("chatzphoto")) {
            // New Chat photo Rec!!!!!!!!
            SenzPhotoHandler.getInstance().onNewChatPhoto(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("profilezphoto")) {
            // New Profile photo Rec!!!!!!!!
            SenzPhotoHandler.getInstance().onNewProfilePhoto(senz, serviceConnection.getInterface(), dbSource, context);
        } else if (senz.getAttributes().containsKey("chatzsound")) {
            // New Sound Rec!!!!!!!!
            SenzSoundHandler.getInstance().onNewChatSound(senz, serviceConnection.getInterface(), dbSource, context);
        } else {
            broadcastDataSenz(senz, context);
        }
    }

    private void handleStream(String stream) {
        if (senzStream != null && senzStream.isActive()) {
            if (stream.contains("#stream off")) {
                Log.d(TAG, "Stream OFFFFFFFFFFFF ");
                senzStream.setIsActive(false);
                //Stream has ended. Already receieved Stream oFF.. we need to notify the handleSenz method to process the stream
                handleSenz(senzStream.getSenzString());
            } else {
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
     *
     * @param stream
     */
    private void setStreamType(String stream) {
        if (senzStream.getStreamType() == null) {
            if (stream.contains("#chatzphoto")) {
                senzStream.setStreamType(SenzStream.SENZ_STEAM_TYPE.CHATZPHOTO);
            } else if (stream.contains("#profilezphoto")) {
                senzStream.setStreamType(SenzStream.SENZ_STEAM_TYPE.PROFILEZPHOTO);
            } else if (stream.contains("#chatzsound")) {
                senzStream.setStreamType(SenzStream.SENZ_STEAM_TYPE.CHATZSOUND);
            }
        }
    }

    private void handleStreamSenz(Senz senz) {
        if (senz.getAttributes().containsKey("stream") && senz.getAttributes().get("stream").equalsIgnoreCase("on")) {
            // stream on, first stream
            Log.d(TAG, "stream ON from " + senz.getSender().getUsername());
            stream = new Stream(true, senz.getSender().getUsername(), new StringBuffer());
        } else if (senz.getAttributes().containsKey("stream") && senz.getAttributes().get("stream").equalsIgnoreCase("off")) {
            // stream off, last stream
            Log.d(TAG, "stream OFF from " + senz.getSender().getUsername());
            stream.setActive(false);

            // TODO handle it
            HashMap<String, String> attrbutes = new HashMap<>();
            attrbutes.put("chatzphoto", stream.getStream());
            attrbutes.put("uid", senz.getAttributes().get("uid"));

            Log.d(TAG, "chatzphoto: " + attrbutes.get("chatzphoto"));

            Senz streamSenz = new Senz("_id", "_signature", SenzTypeEnum.STREAM, senz.getSender(), senz.getReceiver(), attrbutes);
            SenzPhotoHandler.getInstance().onNewChatPhoto(streamSenz, serviceConnection.getInterface(), dbSource, context);
        } else {
            // middle stream
            Log.d(TAG, "stream mid from " + senz.getSender().getUsername() + "data " + senz.getAttributes().get("chatzphoto"));
            stream.appendStream(senz.getAttributes().get("chatzphoto"));
        }
    }

    public SenzServiceConnection getServiceConnection() {
        return serviceConnection;
    }
}

