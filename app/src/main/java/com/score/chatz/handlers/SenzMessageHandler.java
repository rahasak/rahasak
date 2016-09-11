package com.score.chatz.handlers;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.RemoteException;
import android.util.Log;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.interfaces.IComHandler;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class SenzMessageHandler extends BaseHandler implements IComHandler {
    private static final String TAG = SenzMessageHandler.class.getName();
    private static SenzMessageHandler instance;

    /**
     * Singleton
     *
     * @return
     */
    public static SenzMessageHandler getInstance() {

            if (instance == null) {
                instance = new SenzMessageHandler();
            }

        return instance;
    }

    @Override
    public void handleSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context) {
        // save senz in db
        User sender = dbSource.getOrCreateUser(senz.getSender().getUsername());

        try {
            Log.d(TAG, "save incoming chatz");
            String msg = URLDecoder.decode(senz.getAttributes().get("chatzmsg"), "UTF-8");
            Secret newSecret = new Secret(msg, null, null,senz.getSender(), senz.getReceiver());
            newSecret.setID(SenzUtils.getUniqueRandomNumber().toString());
            Long _timeStamp = System.currentTimeMillis();
            newSecret.setTimeStamp(_timeStamp);
            dbSource.createSecret(newSecret);

            senz.setSender(sender);
            Log.d(TAG, "save messages");
            // if senz already exists in the db, SQLiteConstraintException should throw

            NotificationUtils.showNotification(context, context.getString(R.string.new_senz), "New message received from @" + senz.getSender().getUsername());
            sendConfirmation(senz, senzService, senz.getReceiver(), true);
            broadcastDataSenz(senz, context);
        } catch (SQLiteConstraintException | UnsupportedEncodingException e) {
            sendConfirmation(senz, senzService, senz.getReceiver(), false);
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void sendConfirmation(Senz senz, ISenzService senzService, User receiver, boolean isDone) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

            //This is unique identifier for each message
            senzAttributes.put("uid", senz.getAttributes().get("uid"));
            if (isDone) {
                senzAttributes.put("msg", "MsgSent");
            } else {
                senzAttributes.put("msg", "MsgSentFail");
            }
            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;
            Senz _senz = new Senz(id, signature, senzType, receiver, senz.getSender(), senzAttributes);

            senzService.send(_senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
