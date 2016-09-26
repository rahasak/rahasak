package com.score.chatz.remote;

import android.content.Intent;
import android.util.Log;

import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Stream;
import com.score.chatz.ui.RecordingActivity;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.SenzParser;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

class SenHandler extends BasHandler {
    private static final String TAG = SenHandler.class.getName();

    private static SenHandler instance;

    private Stream stream;

    public static SenHandler getInstance() {
        if (instance == null) {
            instance = new SenHandler();
        }

        return instance;
    }

    public void handle(String senzMsg, SenzService senzService) {
        Senz senz = SenzParser.parse(senzMsg);
        if (senz.getSenzType() != SenzTypeEnum.STREAM)
            SenzTracker.getInstance(senzService).stopSenzTrack(senz);
        switch (senz.getSenzType()) {
            case PING:
                Log.d(TAG, "PING received");
                break;
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
            String title = "@" + senz.getSender().getUsername();
            String message = "You have been invited to share secrets.";
            showStatusNotification(senzService.getApplicationContext(), title, message, senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.MESSAGE);

            // broadcast
            broadcastShareSenz(senz, senzService.getApplicationContext());
        } else {
            // #mic #cam #lat #lon permission
            SenzorsDbSource dbSource = new SenzorsDbSource(senzService.getApplicationContext());
            if (senz.getAttributes().containsKey("cam")) {
                dbSource.updatePermissions(senz.getSender(), senz.getAttributes().get("cam"), null, null);
                showPermissionNotification(senzService.getApplicationContext(), senz.getSender(), "cam", senz.getAttributes().get("cam").equalsIgnoreCase("on"));
            } else if (senz.getAttributes().containsKey("mic")) {
                dbSource.updatePermissions(senz.getSender(), null, null, senz.getAttributes().get("mic"));
                showPermissionNotification(senzService.getApplicationContext(), senz.getSender(), "mic", senz.getAttributes().get("mic").equalsIgnoreCase("on"));
            } else if (senz.getAttributes().containsKey("lat")) {
                dbSource.updatePermissions(senz.getSender(), null, senz.getAttributes().get("lat"), null);
                showPermissionNotification(senzService.getApplicationContext(), senz.getSender(), "loc", senz.getAttributes().get("lat").equalsIgnoreCase("on"));
            }

            // send status
            // broadcast
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "701"));
            broadcastShareSenz(senz, senzService.getApplicationContext());
        }
    }

    private void handleGet(Senz senz, SenzService senzService) {
        if (senz.getAttributes().containsKey("cam")) {
            // TODO send ack 800 first

            // launch camera
            handleCam(senz, senzService);
        } else if (senz.getAttributes().containsKey("mic")) {
            // launch mic
            handleMic(senz, senzService);
        } else if (senz.getAttributes().containsKey("lat")) {
            // handle location

        }
    }

    private void handleData(Senz senz, SenzService senzService) {
        // save in db
        if (senz.getAttributes().containsKey("status")) {
            // status
            // broadcast
            broadcastDataSenz(senz, senzService.getApplicationContext());
        } else if (senz.getAttributes().containsKey("msg")) {
            // rahasa
            // send ack
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "700"));

            try {
                // save and broadcast
                String rahasa = URLDecoder.decode(senz.getAttributes().get("msg"), "UTF-8");
                saveSecret(rahasa, "TEXT", senz.getSender(), senzService.getApplicationContext());
                broadcastDataSenz(senz, senzService.getApplicationContext());

                // show notification
                String title = "@" + senz.getSender().getUsername();
                showStatusNotification(senzService.getApplicationContext(), title, rahasa, senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.MESSAGE);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (senz.getAttributes().containsKey("lat") || senz.getAttributes().containsKey("lon")) {
            // location, broadcast
            broadcastDataSenz(senz, senzService.getApplicationContext());
        }
    }

    private void handleStream(Senz senz, SenzService senzService) {
        if (SenzUtils.isStreamOn(senz)) {
            // stream on, first stream
            Log.d(TAG, "stream ON from " + senz.getSender().getUsername());
            stream = new Stream(true, senz.getSender().getUsername(), new StringBuffer());
        } else if (SenzUtils.isStreamOff(senz)) {
            // stream off, last stream
            Log.d(TAG, "stream OFF from " + senz.getSender().getUsername());
            stream.setActive(false);

            // new stream senz
            HashMap<String, String> attributes = new HashMap<>();
            if (senz.getAttributes().containsKey("cam"))
                attributes.put("cam", stream.getStream());
            else
                attributes.put("mic", stream.getStream());
            attributes.put("uid", senz.getAttributes().get("uid"));
            Senz streamSenz = new Senz("_id", "_signature", SenzTypeEnum.STREAM, senz.getSender(), senz.getReceiver(), attributes);

            // save in db
            // broadcast
            if (senz.getAttributes().containsKey("cam"))
                saveSecret(stream.getStream(), "IMAGE", senz.getSender(), senzService.getApplicationContext());
            else
                saveSecret(stream.getStream(), "SOUND", senz.getSender(), senzService.getApplicationContext());
            broadcastDataSenz(streamSenz, senzService.getApplicationContext());
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
            Intent intent = IntentProvider.getCameraIntent(senzService.getApplicationContext());
            intent.putExtra("Senz", senz);
            senzService.getApplicationContext().startActivity(intent);
        } catch (Exception e) {
            // fail to access camera
            senzService.writeSenz(SenzUtils.getAckSenz(senz.getSender(), senz.getAttributes().get("uid"), "802"));
        }
    }

    private void handleMic(Senz senz, SenzService senzService) {
        Intent openRecordingActivity = new Intent();
        openRecordingActivity.setClass(senzService.getApplicationContext(), RecordingActivity.class);
        openRecordingActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openRecordingActivity.putExtra("SENDER", senz.getSender().getUsername());
        senzService.getApplicationContext().startActivity(openRecordingActivity);
    }

}