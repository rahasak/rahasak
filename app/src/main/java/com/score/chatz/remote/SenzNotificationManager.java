package com.score.chatz.remote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.score.chatz.R;
import com.score.chatz.application.SenzApplication;
import com.score.chatz.enums.NotificationType;
import com.score.chatz.pojo.SenzNotification;
import com.score.chatz.ui.ChatActivity;
import com.score.chatz.ui.HomeActivity;
import com.score.chatz.utils.NotificationUtils;

class SenzNotificationManager {

    private Context context;
    private static SenzNotificationManager instance;

    private SenzNotificationManager(Context context) {
        this.context = context;
    }

    static SenzNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new SenzNotificationManager(context);
        }

        return instance;
    }

    /**
     * Get notification to create/ update
     * We need to create or update notification in different scenarios
     *
     * @return notification
     */
    private Notification getNotification(SenzNotification senzNotification) {

        // set up pending intent
        Intent intent;
        if (senzNotification.getNotificationType() == NotificationType.NEW_SECRET) {
            intent = new Intent(context, ChatActivity.class);
            intent.putExtra("SENDER", senzNotification.getSender());
        } else {
            intent = new Intent(context, HomeActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(senzNotification.getTitle())
                .setContentText(senzNotification.getMessage())
                .setSmallIcon(senzNotification.getIcon())
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);

        if (senzNotification.getNotificationType() != NotificationType.NEW_SECRET) {
            builder.setVibrate(new long[]{1000, 1000});
        }

        Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notification);
        builder.setSound(sound);

        return builder.build();
    }

    /**
     * Display notification from here
     *
     * @param senzNotification
     */
    void showNotification(SenzNotification senzNotification) {
        if (senzNotification.getNotificationType() == NotificationType.NEW_PERMISSION || senzNotification.getNotificationType() == NotificationType.NEW_USER) {
            Notification notification = getNotification(senzNotification);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NotificationUtils.MESSAGE_NOTIFICATION_ID, notification);
        } else if (senzNotification.getNotificationType() == NotificationType.NEW_SECRET) {
            //if (NotificationUtils.isBackgroundRunning(context) || !NotificationUtils.isAppInteractable(context) || NotificationUtils.isScreenLocked(context)) {
            if (!SenzApplication.isOnChat()) {
                // display other types of notification when user not on chat
                Notification notification = getNotification(senzNotification);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NotificationUtils.MESSAGE_NOTIFICATION_ID, notification);
            }
        }
    }

    /**
     * Create and update notification when query receives from server
     * No we have two notifications regarding Sensor application
     *
     * @param message incoming query
     */
    void updateNotification(Context context, String message) {
        /*Notification notification = getNotification(context, R.drawable.logo_green, context.getString(R.string.new_senz), message);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MESSAGE_NOTIFICATION_ID, notification);*/
    }

    /**
     * Cancel notification
     * need to cancel when disconnect from web socket
     */
    void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NotificationUtils.MESSAGE_NOTIFICATION_ID);
    }

}
