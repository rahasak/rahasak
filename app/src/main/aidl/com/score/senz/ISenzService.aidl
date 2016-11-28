package com.score.senz;

import com.score.senzc.pojos.Senz;


interface ISenzService {
    void send(in Senz senz);

    void sendInOrder(in List<Senz> senzList);

    void sendFromUri(in String Uri, in Senz originalSenz, in String uid);
}
