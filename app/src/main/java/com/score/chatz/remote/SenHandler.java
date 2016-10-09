package com.score.chatz.remote;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.Stream;
import com.score.chatz.services.LocationService;
import com.score.chatz.ui.PhotoActivity;
import com.score.chatz.ui.RecordingActivity;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.SenzParser;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

class SenHandler {
    private static final String TAG = SenHandler.class.getName();

    private static SenHandler instance;

    private Stream stream;

    static SenHandler getInstance() {
        if (instance == null) {
            instance = new SenHandler();
        }

        return instance;
    }

    void handle(String senzMsg, SenzService senzService) {
        Senz senz = SenzParser.parse(senzMsg);
        //if (senz.getSenzType() != SenzTypeEnum.STREAM)
        //SenzTracker.getInstance(senzService).stopSenzTrack(senz);
        switch (senz.getSenzType()) {
            case SHARE:
                Log.d(TAG, "SHARE received");
                handleShare(senz, senzService);
                break;
            case GET:
                Log.d(TAG, "GET received");
                handleGet(senz, senzService);
                break;
            case DATA:
                Log.d(TAG, "DATA received");
                handleData(senz, senzService);
                break;
            case STREAM:
                Log.d(TAG, "STREAM received");
                handleStream(senz, senzService);
                break;
        }
    }

    private void handleShare(Senz senz, SenzService senzService) {
        if (senz.getAttributes().containsKey("msg") && senz.getAttributes().containsKey("status")) {
            // new user
            // new user permissions, save to db
            SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());
            dbSource.getOrCreateUser(senz.getSender().getUsername());
            dbSource.createPermissionsForUser(senz);
            dbSource.createConfigurablePermissionsForUser(senz);

            // send ack
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "701"));

            // show notification to current user
            SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                    NotificationUtils.getNewUserNotification(senz.getSender().getUsername()));

            // broadcast
            broadcastSenz(senz, senzService.getApplicationContext());
        } else {
            // #mic #cam #lat #lon permission
            SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());
            if (senz.getAttributes().containsKey("cam")) {
                dbSource.updatePermissions(senz.getSender(), senz.getAttributes().get("cam"), null, null);
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(senz.getSender().getUsername(), "cam", senz.getAttributes().get("cam")));
            } else if (senz.getAttributes().containsKey("mic")) {
                dbSource.updatePermissions(senz.getSender(), null, null, senz.getAttributes().get("mic"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(senz.getSender().getUsername(), "mic", senz.getAttributes().get("mic")));
            } else if (senz.getAttributes().containsKey("lat")) {
                dbSource.updatePermissions(senz.getSender(), null, senz.getAttributes().get("lat"), null);
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(senz.getSender().getUsername(), "lat", senz.getAttributes().get("lat")));
            }

            // send status
            // broadcast
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "701"));
            broadcastSenz(senz, senzService.getApplicationContext());
        }
    }

    private void handleGet(Senz senz, SenzService senzService) {
        if (senz.getAttributes().containsKey("cam")) {
            // send ack back
            senzService.writeSenz(SenzUtils.getAckSenz(new User("", "senzswitch"), senz.getAttributes().get("uid"), "DELIVERED"));

            // launch camera
            handleCam(senz, senzService);
        } else if (senz.getAttributes().containsKey("mic")) {
            // send ack back
            senzService.writeSenz(SenzUtils.getAckSenz(new User("", "senzswitch"), senz.getAttributes().get("uid"), "DELIVERED"));

            // launch mic
            handleMic(senz, senzService);
        } else if (senz.getAttributes().containsKey("lat")) {
            // handle location
            handleLocation(senz, senzService);
        }
    }

    private void handleData(Senz senz, SenzService senzService) {
        // save in db
        if (senz.getAttributes().containsKey("status")) {
            // status coming from switch
            // broadcast
            broadcastSenz(senz, senzService.getApplicationContext());
        } else if (senz.getAttributes().containsKey("msg")) {
            // rahasa
            // send ack
            senzService.writeSenz(SenzUtils.getAckSenz(new User("", "senzswitch"), senz.getAttributes().get("uid"), "DELIVERED"));

            try {
                // save and broadcast
                String rahasa = URLDecoder.decode(senz.getAttributes().get("msg"), "UTF-8");
                saveSecret(rahasa, "TEXT", senz.getSender(), senzService.getApplicationContext());
                broadcastSenz(senz, senzService.getApplicationContext());

                // show notification
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getNewSecretNotification(senz.getSender().getUsername(), rahasa));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (senz.getAttributes().containsKey("lat") || senz.getAttributes().containsKey("lon")) {
            // location, broadcast
            broadcastSenz(senz, senzService.getApplicationContext());
        }
    }

    private void handleStream(Senz senz, SenzService senzService) {
        if (SenzUtils.isStreamOn(senz)) {
            // stream on, first stream
            Log.d(TAG, "stream ON from " + senz.getSender().getUsername());
            stream = new Stream(senz.getSender().getUsername());
        } else if (SenzUtils.isStreamOff(senz)) {
            // stream off, last stream
            Log.d(TAG, "stream OFF from " + senz.getSender().getUsername());

            // send status back first
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "DELIVERED"));

            // new stream senz
            HashMap<String, String> attributes = new HashMap<>();
            if (senz.getAttributes().containsKey("cam"))
                attributes.put("cam", stream.getStream());
            else
                attributes.put("mic", stream.getStream());

            attributes.put("uid", senz.getAttributes().get("uid"));
            attributes.put("time", senz.getAttributes().get("time"));

            Senz streamSenz = new Senz("_id", "_signature", SenzTypeEnum.STREAM, senz.getSender(), senz.getReceiver(), attributes);

            Log.d(TAG, "stream ---- " + stream.getStream());

            // save in db
            // broadcast
            if (senz.getAttributes().containsKey("cam"))
                saveSecret(stream.getStream(), "IMAGE", senz.getSender(), senzService.getApplicationContext());
            else
                saveSecret(stream.getStream(), "SOUND", senz.getSender(), senzService.getApplicationContext());
            broadcastSenz(streamSenz, senzService.getApplicationContext());
        } else {
            // middle stream
            if (senz.getAttributes().containsKey("cam"))
                stream.appendStream(senz.getAttributes().get("cam"));
            else
                stream.appendStream(senz.getAttributes().get("mic"));
        }
    }

    private void handleCam(Senz senz, SenzService senzService) {
        try {
            Intent intent = new Intent(senzService.getApplicationContext(), PhotoActivity.class);
            intent.putExtra("USER", senz.getSender());
            senzService.getApplicationContext().startActivity(intent);
        } catch (Exception e) {
            // fail to access camera
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "802"));
        }
    }

    private void handleMic(Senz senz, SenzService senzService) {
        Intent intent = new Intent();
        intent.setClass(senzService.getApplicationContext(), RecordingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("USER", senz.getSender());
        senzService.getApplicationContext().startActivity(intent);
    }

    private void handleLocation(Senz senz, SenzService senzService) {
        Intent intent = new Intent(senzService.getApplicationContext(), LocationService.class);
        intent.putExtra("SENZ", senz);
        senzService.getApplicationContext().startService(intent);
    }

    private void broadcastSenz(Senz senz, Context context) {
        Intent intent = new Intent("com.score.chatz.SENZ");
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    private void saveSecret(String blob, String type, User user, final Context context) {
        // create secret
        final Secret secret = new Secret(blob, type, user, true);
        Long timestamp = (System.currentTimeMillis() / 1000);
        secret.setId(SenzUtils.getUid(context, timestamp.toString()));
        secret.setTimeStamp(timestamp);
        secret.setMissed(false);

        // save secret async
        new Thread(new Runnable() {
            @Override
            public void run() {
                new SenzorsDbSource(context).createSecret(secret);
            }
        }).start();
    }

}
