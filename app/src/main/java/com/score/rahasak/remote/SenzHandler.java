package com.score.rahasak.remote;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;
import com.score.rahasak.pojo.Secret;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.pojo.Stream;
import com.score.rahasak.ui.SecretRecordingActivity;
import com.score.rahasak.ui.SelfieCaptureActivity;
import com.score.rahasak.utils.CryptoUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NotificationUtils;
import com.score.rahasak.utils.PhoneBookUtil;
import com.score.rahasak.utils.SenzParser;
import com.score.rahasak.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.net.URLDecoder;
import java.util.HashMap;

class SenzHandler {
    private static final String TAG = SenzHandler.class.getName();

    private static SenzHandler instance;

    private Stream stream;

    static SenzHandler getInstance() {
        if (instance == null) {
            instance = new SenzHandler();
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
                SecretUser secretUser = dbSource.getSecretUser(username);
                if (secretUser != null) {
                    String encryptedSessionKey = senz.getAttributes().get("$skey");
                    String sessionKey = CryptoUtils.decryptRSA(CryptoUtils.getPrivateKey(senzService.getApplicationContext()), encryptedSessionKey);
                    dbSource.updateSecretUser(username, "session_key", sessionKey);
                } else {
                    secretUser = new SecretUser(senz.getSender().getId(), senz.getSender().getUsername());
                    dbSource.createSecretUser(secretUser);
                }

                // activate user
                dbSource.activateSecretUser(username, true);

                // notification user
                String notificationUser = username;
                if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                    String contactName = PhoneBookUtil.getContactName(senzService, secretUser.getPhone());
                    notificationUser = contactName + "(@" + username + ")";
                }
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(NotificationUtils.getUserNotification(notificationUser));

                // broadcast send status back
                broadcastSenz(senz, senzService.getApplicationContext());
                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "USER_SHARED"));
            } catch (Exception ex) {
                ex.printStackTrace();

                // send error ack
                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "USER_SHARE_FAILED"));
            }
        } else if (senz.getAttributes().containsKey("$skey")) {
            // re sharing session key
            // broadcast send status back
            SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());
            try {
                if (dbSource.isExistingUser(senz.getSender().getUsername())) {
                    String encryptedSessionKey = senz.getAttributes().get("$skey");
                    String sessionKey = CryptoUtils.decryptRSA(CryptoUtils.getPrivateKey(senzService.getApplicationContext()), encryptedSessionKey);
                    dbSource.updateSecretUser(senz.getSender().getUsername(), "session_key", sessionKey);

                    broadcastSenz(senz, senzService.getApplicationContext());
                    senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "KEY_SHARED"));
                } else {
                    // means error
                    senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "KEY_SHARE_FAILED"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();

                // send error ack
                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "KEY_SHARE_FAILED"));
            }
        } else {
            // #mic #cam #lat #lon permission
            SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());
            SecretUser secretUser = dbSource.getSecretUser(senz.getSender().getUsername());

            // notification user
            String notificationUser = secretUser.getUsername();
            if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                String contactName = PhoneBookUtil.getContactName(senzService, secretUser.getPhone());
                notificationUser = contactName + "(@" + secretUser.getUsername() + ")";
            }

            if (senz.getAttributes().containsKey("cam")) {
                dbSource.updatePermission(secretUser.getRecvPermission().getId(), "cam", senz.getAttributes().get("cam").equalsIgnoreCase("on"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(notificationUser, "camera", senz.getAttributes().get("cam")));
            } else if (senz.getAttributes().containsKey("mic")) {
                dbSource.updatePermission(secretUser.getRecvPermission().getId(), "mic", senz.getAttributes().get("mic").equalsIgnoreCase("on"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(notificationUser, "mic", senz.getAttributes().get("mic")));
            } else if (senz.getAttributes().containsKey("lat")) {
                dbSource.updatePermission(secretUser.getRecvPermission().getId(), "loc", senz.getAttributes().get("lat").equalsIgnoreCase("on"));
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getPermissionNotification(notificationUser, "location", senz.getAttributes().get("lat")));
            }

            // send status
            // broadcast
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "PERMISSION_SHARED"));
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

            String status = senz.getAttributes().get("status");
            if (status.equalsIgnoreCase("USER_SHARED")) {
                // user added successfully
                // save user in db
                String username = senz.getSender().getUsername();
                SecretUser secretUser = dbSource.getSecretUser(username);
                if (secretUser != null) {
                    // existing user, activate it
                    dbSource.activateSecretUser(senz.getSender().getUsername(), true);
                } else {
                    // not existing user
                    // this is when sharing directly by username
                    // create and activate uer
                    secretUser = new SecretUser("id", senz.getSender().getUsername());
                    dbSource.createSecretUser(secretUser);
                    dbSource.activateSecretUser(secretUser.getUsername(), true);
                }

                // notification user
                String notificationUser = username;
                if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                    String contactName = PhoneBookUtil.getContactName(senzService, secretUser.getPhone());
                    notificationUser = contactName + "(@" + username + ")";
                }
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(NotificationUtils.getUserConfirmNotification(notificationUser));
            }

            broadcastSenz(senz, senzService.getApplicationContext());
        } else if (senz.getAttributes().containsKey("msg") || senz.getAttributes().containsKey("$msg")) {
            // rahasa
            // send ack
            senzService.writeSenz(SenzUtils.getAckSenz(new User("", "senzswitch"), senz.getAttributes().get("uid"), "DELIVERED"));

            try {
                // save and broadcast
                String rahasa;
                if (senz.getAttributes().containsKey("$msg")) {
                    // encrypted data -> decrypt
                    String sessionKey = dbSource.getSecretUser(senz.getSender().getUsername()).getSessionKey();
                    rahasa = CryptoUtils.decryptECB(CryptoUtils.getSecretKey(sessionKey), senz.getAttributes().get("$msg"));
                } else {
                    // plain data
                    rahasa = URLDecoder.decode(senz.getAttributes().get("msg"), "UTF-8");
                }

                Long timestamp = (System.currentTimeMillis() / 1000);
                saveSecret(timestamp, senz.getAttributes().get("uid"), rahasa, BlobType.TEXT, senz.getSender(), false, senzService.getApplicationContext());
                senz.getAttributes().put("time", timestamp.toString());
                senz.getAttributes().put("msg", rahasa);
                broadcastSenz(senz, senzService.getApplicationContext());

                // notification user
                String username = senz.getSender().getUsername();
                SecretUser secretUser = dbSource.getSecretUser(username);
                String notificationUser = secretUser.getUsername();
                if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                    String contactName = PhoneBookUtil.getContactName(senzService, secretUser.getPhone());
                    notificationUser = contactName + "(@" + username + ")";
                }

                // show notification
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getSecretNotification(notificationUser, username, rahasa));
            } catch (Exception e) {
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
                    String sessionKey = CryptoUtils.getSessionKey();
                    dbSource.updateSecretUser(username, "session_key", sessionKey);

                    String encryptedSessionKey = CryptoUtils.encryptRSA(CryptoUtils.getPublicKey(pubKey), sessionKey);
                    senzService.writeSenz(SenzUtils.getShareSenz(senzService.getApplicationContext(), username, encryptedSessionKey));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (senz.getAttributes().containsKey("mic")) {
            broadcastSenz(senz, senzService.getApplicationContext());
        } else if (senz.getAttributes().containsKey("senz")) {
            String senzMsg = new String(Base64.decode(senz.getAttributes().get("senz"), Base64.DEFAULT));
            Senz innerSenz = SenzParser.parse(senzMsg);

            senzService.writeSenz(SenzUtils.getAckSenz(new User("", "senzswitch"), innerSenz.getAttributes().get("uid"), "DELIVERED"));
            if (innerSenz.getAttributes().containsKey("cam")) {
                // selfie mis
                Long timestamp = (System.currentTimeMillis() / 1000);
                saveSecret(timestamp, innerSenz.getAttributes().get("uid"), "", BlobType.IMAGE, innerSenz.getSender(), true, senzService.getApplicationContext());

                // notification user
                String username = innerSenz.getSender().getUsername();
                SecretUser secretUser = dbSource.getSecretUser(username);
                String notificationUser = secretUser.getUsername();
                if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                    String contactName = PhoneBookUtil.getContactName(senzService, secretUser.getPhone());
                    notificationUser = contactName + "(@" + username + ")";
                }

                // show notification
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getStreamNotification(notificationUser, "Missed selfie call", username));
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

            // handle for cam
            if (senz.getAttributes().containsKey("cam")) {
                // send status back first
                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "DELIVERED"));

                // new stream
                HashMap<String, String> attributes = new HashMap<>();
                Long timestamp = (System.currentTimeMillis() / 1000);
                attributes.put("cam", stream.getStream());
                attributes.put("uid", senz.getAttributes().get("uid"));
                attributes.put("time", timestamp.toString());

                // save
                saveSecret(timestamp, senz.getAttributes().get("uid"), "", BlobType.IMAGE, senz.getSender(), false, senzService.getApplicationContext());
                String imgName = senz.getAttributes().get("uid") + ".jpg";
                ImageUtils.saveImg(imgName, stream.getStream());

                Senz streamSenz = new Senz("_id", "_signature", SenzTypeEnum.STREAM, senz.getSender(), senz.getReceiver(), attributes);
                broadcastSenz(streamSenz, senzService.getApplicationContext());

                // notification user
                String username = senz.getSender().getUsername();
                SecretUser secretUser = new SenzorsDbSource(senzService.getApplicationContext()).getSecretUser(username);
                String notificationUser = secretUser.getUsername();
                if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                    String contactName = PhoneBookUtil.getContactName(senzService, secretUser.getPhone());
                    notificationUser = contactName + "(@" + username + ")";
                }

                // show notification
                SenzNotificationManager.getInstance(senzService.getApplicationContext()).showNotification(
                        NotificationUtils.getStreamNotification(notificationUser, "Received new selfie", username));
            }
        } else {
            // middle stream
            if (senz.getAttributes().containsKey("cam")) {
                stream.appendStream(senz.getAttributes().get("cam"));
            }
        }
    }

    private void handleCam(Senz senz, SenzService senzService) {
        if (!SenzApplication.isOnCall()) {
            try {
                Intent intent = new Intent(senzService.getApplicationContext(), SelfieCaptureActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("USER", senz.getSender().getUsername());
                senzService.getApplicationContext().startActivity(intent);
            } catch (Exception e) {
                // fail to access camera
                senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "CAM_ERROR"));
            }
        } else {
            // user in another call
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "CAM_BUSY"));
        }
    }

    private void handleMic(Senz senz, SenzService senzService) {
        if (!SenzApplication.isOnCall()) {
            Intent intent = new Intent();
            intent.setClass(senzService.getApplicationContext(), SecretRecordingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("USER", senz.getSender().getUsername());
            senzService.getApplicationContext().startActivity(intent);
        } else {
            // user in another call
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "BUSY"));
        }
    }

    private void handleLocation(Senz senz, SenzService senzService) {
        Intent intent = new Intent(senzService.getApplicationContext(), LatLonService.class);
        intent.putExtra("SENZ", senz);
        senzService.getApplicationContext().startService(intent);
    }

    private void broadcastSenz(Senz senz, Context context) {
        Intent intent = new Intent("com.score.rahasak.SENZ");
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    private void saveSecret(Long timestamp, String uid, String blob, BlobType blobType, User user, boolean missed, final Context context) {
        // create secret
        try {
            final Secret secret = new Secret(blob, blobType, new SecretUser(user.getId(), user.getUsername()), true);
            secret.setId(uid);
            secret.setTimeStamp(timestamp);
            secret.setMissed(missed);
            secret.setDeliveryState(DeliveryState.NONE);
            new SenzorsDbSource(context).createSecret(secret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(Senz senz, final Context context) {
        try {
            final String uid = senz.getAttributes().get("uid");
            String status = senz.getAttributes().get("status");
            if (status.equalsIgnoreCase("DELIVERED")) {
                new SenzorsDbSource(context).updateDeliveryStatus(DeliveryState.DELIVERED, uid);
            } else if (status.equalsIgnoreCase("RECEIVED")) {
                new SenzorsDbSource(context).updateDeliveryStatus(DeliveryState.RECEIVED, uid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
