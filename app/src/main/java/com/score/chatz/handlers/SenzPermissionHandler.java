package com.score.chatz.handlers;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.RemoteException;
import android.util.Log;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.interfaces.IDataPermissionSenzHandler;
import com.score.chatz.interfaces.IReceivingComHandler;
import com.score.chatz.interfaces.ISendAckHandler;
import com.score.chatz.utils.NotificationUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class SenzPermissionHandler extends BaseHandler implements ISendAckHandler, IDataPermissionSenzHandler {

    private static final String TAG = SenzPermissionHandler.class.getName();

    private static SenzPermissionHandler instance;

    /**
     * Singleton
     *
     * @return
     */
    public static SenzPermissionHandler getInstance() {
        if (instance == null) {
            instance = new SenzPermissionHandler();
        }
        return instance;
    }


    public void handleSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {

    }

    @Override
    public void sendConfirmation(Senz senz, ISenzService senzService, User receiver, boolean isDone) {
        Log.d(TAG, "send response(shareback) for permission");
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            if (isDone) {
                senzAttributes.put("msg", "SharePermDone");
                if (senz.getAttributes().containsKey("locPerm")) {
                    senzAttributes.put("locPerm", senz.getAttributes().get("locPerm"));
                }
                if (senz.getAttributes().containsKey("camPerm")) {
                    senzAttributes.put("camPerm", senz.getAttributes().get("camPerm"));
                }
                if (senz.getAttributes().containsKey("micPerm")) {
                    senzAttributes.put("micPerm", senz.getAttributes().get("micPerm"));
                }
            } else {
                senzAttributes.put("msg", "ShareFail");
            }

            String id = "_ID";
            String signature = "";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;
            Senz newSenz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

            senzService.send(newSenz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewPermission(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        // save senz in db
        User sender = dbSource.getOrCreateUser(senz.getSender().getUsername());
        try {

            if (senz.getAttributes().containsKey("locPerm")) {
                dbSource.updatePermissions(senz.getSender(), null, senz.getAttributes().get("locPerm"), null);
                if(senz.getAttributes().get("locPerm").equalsIgnoreCase("true")) {
                    NotificationUtils.showNotification(context, "@" + senz.getSender().getUsername(), "You been granted location permission!", senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
                }else{
                    NotificationUtils.showNotification(context, "@" + senz.getSender().getUsername(), "Your location privilege has been revoked!", senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
                }
            }
            if (senz.getAttributes().containsKey("camPerm")) {
                dbSource.updatePermissions(senz.getSender(), senz.getAttributes().get("camPerm"), null, null);
                if(senz.getAttributes().get("camPerm").equalsIgnoreCase("true")) {
                    NotificationUtils.showNotification(context, "@" + senz.getSender().getUsername(), "You been granted camera permission!", senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
                }else{
                    NotificationUtils.showNotification(context, "@" + senz.getSender().getUsername(), "Your camera privilege has been revoked!", senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
                }
            }
            if (senz.getAttributes().containsKey("micPerm")) {
                dbSource.updatePermissions(senz.getSender(), null, null, senz.getAttributes().get("micPerm"));
                if(senz.getAttributes().get("micPerm").equalsIgnoreCase("true")) {
                    NotificationUtils.showNotification(context, "@" + senz.getSender().getUsername(), "You been granted mic permission!", senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
                }else{
                    NotificationUtils.showNotification(context, "@" + senz.getSender().getUsername(), "Your mic privilege has been revoked!", senz.getSender().getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
                }
            }

            senz.setSender(sender);
            Log.d(TAG, "saving new permissions");



            sendConfirmation(senz, senzService, sender, true);
            broadcastDataSenz(senz, context);
        } catch (SQLiteConstraintException e) {
            sendConfirmation(senz, senzService, sender, false);
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onPermissionSharingDone(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        if (senz.getAttributes().containsKey("msg") && senz.getAttributes().get("msg").equalsIgnoreCase("sharePermDone")) {
            // save senz in db
            User sender = dbSource.getOrCreateUser(senz.getSender().getUsername());
            senz.setSender(sender);
        }
        broadcastDataSenz(senz, context);
    }
}
