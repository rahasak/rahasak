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
        // Handle Notificaiton Action
        if (intent.hasExtra("NOTIFICATION_ACCEPT")) {
            onReceiveAcceptAction(context, intent);
        } else if (intent.hasExtra("NOTIFICATION_DISMISS")) {
            onReceiveDismissAction(context, intent);
        }
    }

    private void onReceiveAcceptAction(Context context, Intent intent) {
        // Notification id. Default is MESSAGE_NOTIFICATION_ID, which is our normal notifications
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", NotificationUtils.MESSAGE_NOTIFICATION_ID);
        String usernameToAdd = intent.getStringExtra("USERNAME_TO_ADD").trim();

        if (new SenzorsDbSource(context).isAddedUser(usernameToAdd)){
            cancelNotification(notificationId, context);
            ActivityUtils.showCustomToast("This user has already been added", context);
        }else if (NetworkUtil.isAvailableNetwork(context)) {
            cancelNotification(notificationId, context);
            broadcastIntentToAddUser(context, usernameToAdd);
        } else {
            ActivityUtils.showCustomToast(context.getResources().getString(R.string.no_internet), context);
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

    // Broadcast intent to service to add user
    private void broadcastIntentToAddUser(Context context, String username) {
        Intent smsReceivedIntent = IntentProvider.getAddUserIntent();
        smsReceivedIntent.putExtra("USERNAME_TO_ADD", username);
        context.sendBroadcast(smsReceivedIntent);
    }
}