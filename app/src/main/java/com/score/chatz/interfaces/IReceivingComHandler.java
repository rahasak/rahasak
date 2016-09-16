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
     * Receive and handle messages
     * @param senz
     * @param senzService
     * @param dbSource
     * @param context
     */
    void handleSenz(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context);

    /**
     * Confirma and reply back
     * @param senzService
     * @param receiver
     * @param isDone
     */
    void sendConfirmation(Senz senz, ISenzService senzService, User receiver, boolean isDone);
}
