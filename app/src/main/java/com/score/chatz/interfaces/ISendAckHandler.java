package com.score.chatz.interfaces;

import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

/**
 * Created by lakmal.caldera on 9/16/2016.
 */
public interface ISendAckHandler {

    /**
     * Confirm and REPLY back where success or fail!!!
     * @param senzService
     * @param receiver
     * @param isDone
     */
    void sendConfirmation(Senz senz, ISenzService senzService, User receiver, boolean isDone);
}
