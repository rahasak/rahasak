package com.score.chatz.handlers;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.score.chatz.asyncTasks.SenzPacketTimeoutTask;
import com.score.chatz.exceptions.NoSenzUidException;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.pojos.Senz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * This class will managa all requests that have been sent with timeoout so it can notify the app the other other user is offline
 * 1. It can take a senz and add a uid if not present and return the senz
 * 2. It can store the senz wait wait for a response with the same uid. If not received before timeout, user is not online
 * Created by Lakmal on 9/12/16.
 */
public class SenzStatusTracker {
    private static final String TAG = SenzStatusTracker.class.getName();
    private static final Integer PACKET_TIMEOUT = 10; // 10 seconds
    private static final String UID = "uid";

    //Store uid -> Senz in pairs, just before sending to other user.
    private static final Map senzDirectory = java.util.Collections.synchronizedMap(new HashMap<String, SenzPacketTimeoutTask>());


    /**
     * Return senz with a uid
     * @param senz
     * @return
     */
    public static Senz addUidToSenz(Senz senz){
        if(!senz.getAttributes().containsKey(UID))
        senz.getAttributes().put(UID, SenzUtils.getUniqueRandomNumber());
        return senz;
    }

    /**
     * Store senz in directory as value and key as its uid
     * @param senz
     */
    public static void addSenz(Senz senz, Context context){
        if(!senzDirectory.containsKey(senz.getAttributes().get(UID))) {
            SenzPacketTimeoutTask timeoutTask = new SenzPacketTimeoutTask(PACKET_TIMEOUT, senz, context);
            senzDirectory.put(senz.getAttributes().get(UID), timeoutTask);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                timeoutTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                timeoutTask.execute();
            Log.i(TAG, "adding to senz directory - " + senz.getAttributes().get(UID) + ", count - " + senzDirectory.size());
        }

    }

    private static void removeSenz(Senz senz) throws NoSenzUidException{
        if(senz.getAttributes().containsKey(UID)) {
            for(Iterator<Map.Entry<String, SenzPacketTimeoutTask>> it = senzDirectory.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, SenzPacketTimeoutTask> entry = it.next();
                if(entry.getKey().equalsIgnoreCase(senz.getAttributes().get(UID))) {
                    entry.getValue().cancel(true);
                    it.remove();
                }
            }
        }else{
            throw new NoSenzUidException();
        }
    }

    /**
     * Remove senz from directory, since it doesn't need to be checked with other incoming requests as it has timedout
     * @param senz
     */
    public static void onTmeout(Senz senz, Context context){
        try {
            removeSenz(senz);
        }catch (NoSenzUidException ex){
            ex.printStackTrace();
        }
        Intent intent = AppIntentHandler.getpacketTimeoutIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    public static void onPacketArrived(final Senz senz){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "removing from senz directory - " + senz.getAttributes().get(UID) + ", count - " + SenzStatusTracker.senzDirectory.size());
                    //((SenzPacketTimeoutTask) senzDirectory.get(senz.getAttributes().get(UID))).cancel(true);
                    try {
                        removeSenz(senz);
                    } catch (NoSenzUidException ex) {
                        ex.printStackTrace();
                    }
            }
        });
    }


}
