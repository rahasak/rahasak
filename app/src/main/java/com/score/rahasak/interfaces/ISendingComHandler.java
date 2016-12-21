package com.score.rahasak.interfaces;

import com.score.senzc.pojos.Senz;

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
