package com.score.chatz.interfaces;

import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.List;

/**
 * Created by Lakmal on 9/15/16.
 */
public interface ISendingComHandler {
    /**
     * Send message to service!!!
     */
    void send(Senz senz);
    void sendInOrder(List<Senz> senzList);

}
