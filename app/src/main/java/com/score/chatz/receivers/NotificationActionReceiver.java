package com.score.chatz.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.ui.AddUserActivity;
import com.score.chatz.ui.HomeActivity;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.NotificationUtils;

/**
 * Created by Lakmal on 11/6/16.
 */

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationActionReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle Dismiss notification with out bringing the app to the foreground
        if(intent.hasExtra("NOTIFICATION_DISMISS")) {
            onReceiveDismissAction(context, intent);
        }
    }
    private void onReceiveDismissAction(Context context, Intent intent) {
        // Notification id
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", NotificationUtils.MESSAGE_NOTIFICATION_ID);
        cancelNotification(notificationId, context);
    }

    // Remove notification from tray
    private void cancelNotification(int NotificationId, Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NotificationId);
    }
}