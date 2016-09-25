package com.score.chatz.remote;

import android.content.Context;
import android.content.Intent;

import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

class BasHandler {
    void broadcastDataSenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getDataSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    void broadcastNewDataToDisplaySenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getNewDataToDisplayIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    void broadcastUserBusySenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getUserBusyIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    void broadcastNoLocationSenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getNoLocationEnabledIntent();
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

    void showNotification(Context context, String title, String body, String sender, NotificationUtils.NOTIFICATION_TYPE type) {
        NotificationUtils.showNotification(context, title, body, sender, type);
    }
}
