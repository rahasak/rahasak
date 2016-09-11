package com.score.chatz.handlers;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.RemoteException;
import android.util.Log;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.interfaces.IComHandler;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class SenzUserHandler extends BaseHandler implements IComHandler {
    private static final String TAG = SenzUserHandler.class.getName();
    private static SenzUserHandler instance;

    /**
     * Singleton
     * @return
     */
    public static SenzUserHandler getInstance(){
        if(instance == null){
            instance = new SenzUserHandler();
        }
        return instance;
    }

    /**
     * Handle new permissions from other users
     *
     * @param senz        Incoming senz with updated permissions
     * @param senzService
     */
    public void handleSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {

        Log.d(TAG, "Saving new user to db");

        User sender = dbSource.getOrCreateUser(senz.getSender().getUsername());
        senz.setSender(sender);
        senz.setId(SenzUtils.getUniqueRandomNumber().toString());

        // if senz already exists in the db, SQLiteConstraintException should throw
        try {
            //1. New user permissions, save to db
            dbSource.createPermissionsForUser(senz);
            dbSource.createConfigurablePermissionsForUser(senz);
            dbSource.createSenz(senz);

            //2. Send confirmation back to sender
            sendConfirmation(null, senzService, sender, true);

            //3. Show notification to current user
            NotificationUtils.showNotification(context, context.getString(R.string.new_senz), "You have received an invitation from @" + senz.getSender().getUsername());

            //4. Broadcast intent to app
            broadcastUpdateSenz(senz, context);
        } catch (SQLiteConstraintException e) {
            sendConfirmation(null, senzService, sender, false);
            Log.e(TAG, e.toString());
        }
    }

    /**
     * After receive invitation from someone, you send back a share message to the user, with your permissions.
     * @param senzService
     * @param receiver
     * @param isDone
     */
    public void sendConfirmation(Senz _senz, ISenzService senzService, User receiver, boolean isDone) {
        Log.d(TAG, "sending response");

        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            if (isDone) {
                senzAttributes.put("msg", "ShareDone");
                senzAttributes.put("lat", "lat");
                senzAttributes.put("lon", "lon");
                // Switch handles the sharing of all attributes
            } else {
                senzAttributes.put("msg", "ShareFail");
            }

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
