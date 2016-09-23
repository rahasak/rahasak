package com.score.chatz.application;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.score.chatz.utils.SenzUtils;
import com.score.senzc.pojos.Senz;

import java.util.HashMap;
import java.util.Map;

/**
 * This class will managa all requests that have been sent with timeoout so it can notify the app the other other user is offline
 * 1. It can take a senz and add a uid if not present and return the senz
 * 2. It can store the senz wait wait for a response with the same uid. If not received before timeout, user is not online
 * Created by Lakmal on 9/12/16.
 */
public class SenzStatusTracker {
    private static final String TAG = SenzStatusTracker.class.getName();
    private static final Integer PACKET_TIMEOUT = 7; // 10 seconds
    private static final String UID = "uid";

    // singleton
    private static Context senzContext;
    private static SenzStatusTracker senzStatusTracker;

    // store uid -> Senz in pairs, just before sending to other user
    private static final Map<String, SenzTimer> senzDirectory = java.util.Collections.synchronizedMap(new HashMap<String, SenzTimer>());

    private SenzStatusTracker() {

    }

    public static SenzStatusTracker getInstance(Context context) {
        if (senzStatusTracker == null) {
            senzContext = context;
            senzStatusTracker = new SenzStatusTracker();
        }

        return senzStatusTracker;
    }

    /**
     * Store senz in directory as value and key as its uid
     *
     * @param senz senz
     */
    public void startSenzTrack(Senz senz) {
        // add UID if not exits
        Senz senzWithUid = putUid(senz);

        // start timer and put on buffer
        SenzTimer timer = new SenzTimer(senz);
        timer.start();
        senzDirectory.put(senzWithUid.getAttributes().get(UID), timer);
    }

    /**
     * Stop track senz
     *
     * @param senz senz
     */
    public void stopSenzTrack(Senz senz) {
        Log.d(TAG, "Response comes for senz with UDI " + senz.getAttributes().get(UID));

        // remove senz
        removeSenz(senz);
    }

    /**
     * Return senz with a uid
     *
     * @param senz senz
     * @return senz with UID
     */
    private Senz putUid(Senz senz) {
        if (!senz.getAttributes().containsKey(UID))
            senz.getAttributes().put(UID, SenzUtils.getUniqueRandomNumber());
        return senz;
    }

    /**
     * Remove senz from map
     *
     * @param senz senz
     */
    private static void removeSenz(Senz senz) {
        // TODO refactor
        SenzTimer timer = senzDirectory.get(senz.getAttributes().get(UID));
        if (timer != null) {
            timer.setBroadcast(false);
            senzDirectory.remove(timer);
        }
    }

    private class SenzTimer extends Thread {
        private Senz senz;
        private boolean broadcast;

        SenzTimer(Senz senz) {
            this.senz = senz;
        }

        public void run() {
            Log.d(TAG, "Start timer for senz with UID " + senz.getAttributes().get(UID));

            // start wait
            try {
                Thread.sleep(PACKET_TIMEOUT * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // wait done, means timeout
            // notify timeout
            onTimeout(senz);
        }

        public void setBroadcast(boolean broadcast) {
            this.broadcast = broadcast;
        }

        private void onTimeout(final Senz senz) {
            Log.d(TAG, "Timeout for senz: " + senz.getAttributes().get(UID) + " broadcast: " + broadcast);
            if (broadcast) {
                removeSenz(senz);

                // broadcast packet
                Intent intent = IntentProvider.getpacketTimeoutIntent();
                intent.putExtra("SENZ", senz);
                senzContext.sendBroadcast(intent);
            }
        }
    }

}
