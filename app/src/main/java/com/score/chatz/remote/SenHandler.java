package com.score.chatz.remote;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.enums.BlobType;
import com.score.chatz.enums.DeliveryState;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.pojo.Stream;
import com.score.chatz.ui.PhotoActivity;
import com.score.chatz.ui.RecordingActivity;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.RSAUtils;
import com.score.chatz.utils.SenzParser;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
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
            try {
                // create user
                String username = senz.getSender().getUsername();
                if (dbSource.isExistingUser(senz.getSender().getUsername())) {
                    String sessionKey = senz.getAttributes().get("skey");
                    dbSource.updateSecretUser(username, "session_key", sessionKey);
                } else {
                    SecretUser secretUser = new SecretUser(senz.getSender().getId(), senz.getSender().getUsername());
                    dbSource.createSecretUser(secretUser);
                }

                // activate user
                dbSource.activateSecretUser(username, true);

                // show notification to current user
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getUserNotification(senz.getSender().getUsername()));

                // broadcast
                broadcastSenz(senz, senzService.getApplicationContext());

                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "701"));
            } catch (Exception ex) {
                // user exists
                ex.printStackTrace();

                // send error ack
                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "702"));
            }
        } else {
            // #mic #cam #lat #lon permission
            SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());
            SecretUser secretUser = dbSource.getSecretUser(senz.getSender().getUsername());
            if (senz.getAttributes().containsKey("cam")) {
                dbSource.updatePermission(secretUser.getRecvPermission().getId(), "cam", senz.getAttributes().get("cam").equalsIgnoreCase("on"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(senz.getSender().getUsername(), "camera", senz.getAttributes().get("cam")));
            } else if (senz.getAttributes().containsKey("mic")) {
                dbSource.updatePermission(secretUser.getRecvPermission().getId(), "mic", senz.getAttributes().get("mic").equalsIgnoreCase("on"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(senz.getSender().getUsername(), "mic", senz.getAttributes().get("mic")));
            } else if (senz.getAttributes().containsKey("lat")) {
                dbSource.updatePermission(secretUser.getRecvPermission().getId(), "loc", senz.getAttributes().get("lat").equalsIgnoreCase("on"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(senz.getSender().getUsername(), "location", senz.getAttributes().get("lat")));
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
        SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());

        if (senz.getAttributes().containsKey("status")) {
            // status coming from switch
            // broadcast
            updateStatus(senz, senzService.getApplicationContext());

            //Only for case of add user, need to handle here to make more generic as you can now add users via sms also, thus cannot gurantee which activity user will be in, when recevie this status code
            String status = senz.getAttributes().get("status");
            if (status.equalsIgnoreCase("701")) {
                // user added successfully
                // save user in db
                if (dbSource.isExistingUser(senz.getSender().getUsername())) {
                    // existing user, activate it
                    dbSource.activateSecretUser(senz.getSender().getUsername(), true);
                } else {
                    // TODO verify and remove this logic
                    // not existing user,
                    // create and activate user
                    SecretUser secretUser = new SecretUser(senz.getSender().getId(), senz.getSender().getUsername());
                    dbSource.createSecretUser(secretUser);
                    dbSource.activateSecretUser(secretUser.getUsername(), true);
                }
            }
            broadcastSenz(senz, senzService.getApplicationContext());
        } else if (senz.getAttributes().containsKey("msg")) {
            // rahasa
            // send ack
            senzService.writeSenz(SenzUtils.getAckSenz(new User("", "senzswitch"), senz.getAttributes().get("uid"), "DELIVERED"));

            try {
                // save and broadcast
                String rahasa = URLDecoder.decode(senz.getAttributes().get("msg"), "UTF-8");

                Long timestamp = (System.currentTimeMillis() / 1000);
                saveSecret(timestamp, senz.getAttributes().get("uid"), rahasa, BlobType.TEXT, senz.getSender(), senzService.getApplicationContext());
                senz.getAttributes().put("time", timestamp.toString());
                broadcastSenz(senz, senzService.getApplicationContext());

                // show notification
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getSecretNotification(senz.getSender().getUsername(), rahasa));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (senz.getAttributes().containsKey("lat") || senz.getAttributes().containsKey("lon")) {
            // location, broadcast
            broadcastSenz(senz, senzService.getApplicationContext());
        } else if (senz.getAttributes().containsKey("pubkey")) {
            // pubkey from switch
            String username = senz.getAttributes().get("name");
            String pubKey = senz.getAttributes().get("pubkey");

            // update pubkey on db
            dbSource.updateSecretUser(username, "pubkey", pubKey);

            // Check if this user is the requester
            SecretUser secretUser = dbSource.getSecretUser(username);
            if (secretUser.isSMSRequester()) {
                try {
                    // create session key for this user
                    String sessionKey = RSAUtils.getSessionKey();
                    dbSource.updateSecretUser(username, "session_key", sessionKey);

                    senzService.writeSenz(SenzUtils.getShareSenz(senzService.getApplicationContext(), senz.getSender().getUsername(), sessionKey));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
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

            Long timestamp = (System.currentTimeMillis() / 1000);
            attributes.put("uid", senz.getAttributes().get("uid"));
            attributes.put("time", timestamp.toString());

            Senz streamSenz = new Senz("_id", "_signature", SenzTypeEnum.STREAM, senz.getSender(), senz.getReceiver(), attributes);

            // save in db
            // broadcast
            if (senz.getAttributes().containsKey("cam"))
                saveSecret(timestamp, senz.getAttributes().get("uid"), stream.getStream(), BlobType.IMAGE, senz.getSender(), senzService.getApplicationContext());
            else
                saveSecret(timestamp, senz.getAttributes().get("uid"), stream.getStream(), BlobType.SOUND, senz.getSender(), senzService.getApplicationContext());

            broadcastSenz(streamSenz, senzService.getApplicationContext());

            // show notification
            SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                    NotificationUtils.getStreamNotification(senz.getSender().getUsername(), senz.getAttributes().containsKey("cam")));
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("USER", senz.getSender().getUsername());
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
        intent.putExtra("USER", senz.getSender().getUsername());
        senzService.getApplicationContext().startActivity(intent);
    }

    private void handleLocation(Senz senz, SenzService senzService) {
        Intent intent = new Intent(senzService.getApplicationContext(), LatLonService.class);
        intent.putExtra("SENZ", senz);
        senzService.getApplicationContext().startService(intent);
    }

    private void broadcastSenz(Senz senz, Context context) {
        Intent intent = new Intent("com.score.chatz.SENZ");
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    private void saveSecret(Long timestamp, String uid, String blob, BlobType blobType, User user, final Context context) {
        // create secret
        final Secret secret = new Secret(blob, blobType, new SecretUser(user.getId(), user.getUsername()), true);
        secret.setId(uid);
        secret.setTimeStamp(timestamp);
        secret.setMissed(false);
        secret.setDeliveryState(DeliveryState.NONE);

        // save secret async
        new Thread(new Runnable() {
            @Override
            public void run() {
                new SenzorsDbSource(context).createSecret(secret);
            }
        }).start();
    }

    private void updateStatus(Senz senz, final Context context) {
        final String uid = senz.getAttributes().get("uid");
        String status = senz.getAttributes().get("status");
        if (status.equalsIgnoreCase("DELIVERED")) {
            // update status in db
            new Thread(new Runnable() {
                @Override
                public void run() {
                    new SenzorsDbSource(context).updateDeliveryStatus(DeliveryState.DELIVERED, uid);
                }
            }).start();
        }
    }
}
