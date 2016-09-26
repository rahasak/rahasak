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

    void broadcastShareSenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getShareSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    void broadcastStreamSenz(Senz senz, Context context) {
        Intent intent = IntentProvider.getStreamSenzIntent();
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

    void showStatusNotification(Context context, String title, String body, String sender, NotificationUtils.NOTIFICATION_TYPE type) {
        NotificationUtils.showNotification(context, title, body, sender, type);
    }

    void showPermissionNotification(Context context, User user, String permissionName, boolean isEnabled) {
        if (permissionName.equalsIgnoreCase("lat")) {
            if (isEnabled)
                NotificationUtils.showNotification(context, "@" + user.getUsername(), "You been granted location permission!", user.getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
            else
                NotificationUtils.showNotification(context, "@" + user.getUsername(), "Your location privilege has been revoked!", user.getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
        } else if (permissionName.equalsIgnoreCase("cam")) {
            if (isEnabled)
                NotificationUtils.showNotification(context, "@" + user.getUsername(), "You been granted camera permission!", user.getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
            else
                NotificationUtils.showNotification(context, "@" + user.getUsername(), "Your camera privilege has been revoked!", user.getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
        } else if (permissionName.equalsIgnoreCase("mic")) {
            if (isEnabled)
                NotificationUtils.showNotification(context, "@" + user.getUsername(), "You been granted mic permission!", user.getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
            else
                NotificationUtils.showNotification(context, "@" + user.getUsername(), "Your mic privilege has been revoked!", user.getUsername(), NotificationUtils.NOTIFICATION_TYPE.PERMISSION);
        }
    }
}
