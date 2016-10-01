package com.score.chatz.remote;

import android.content.Context;
import android.content.Intent;

import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

class BasHandler {
    void broadcastSenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    void saveSecret(String blob, String type, User user, final Context context) {
        // create secret
        final Secret secret = new Secret(blob, type, user, true);
        secret.setID(SenzUtils.getUniqueRandomNumber());
        secret.setTimeStamp(System.currentTimeMillis());

        // save secret async
        new Thread(new Runnable() {
            @Override
            public void run() {
                new SenzorsDbSource(context).createSecret(secret);
            }
        }).start();
    }

}
