package com.score.chatz.interfaces;

import android.content.Context;

import com.score.chatz.db.SenzorsDbSource;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

/**
 * All objects implmenting this interface can handle incomming messages from the server.
 * After receiving message, also support sending confirmation to the other party to notify message successfully received!!!
 * Created by Lakmal on 9/4/16.
 */
public interface IReceivingComHandler {
    /**
     * Receive and handle GET messages
     * @param senz
     * @param senzService
     * @param dbSource
     * @param context
     */
    void handleGetSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context);

    /**
     * Receive and handle SHARE messages
     * @param senz
     * @param senzService
     * @param dbSource
     * @param context
     */
    void handleShareSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context);

    // Note - Get Requests are handle explicitly using IData Interfaces!!
}
