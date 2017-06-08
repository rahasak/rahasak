package com.score.senz;

import com.score.senzc.pojos.Senz;


interface ISenzService {
    void send(in Senz senz);

    void sendStream(in List<Senz> senzList);
}
