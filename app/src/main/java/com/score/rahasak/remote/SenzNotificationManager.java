package com.score.rahasak.remote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.application.SenzApplication;
import com.score.rahasak.enums.NotificationType;
import com.score.rahasak.pojo.SenzNotification;
import com.score.rahasak.ui.ChatActivity;
import com.score.rahasak.ui.DrawerActivity;

public class SenzNotificationManager {

    private static final String TAG = SenzNotificationManager.class.getName();

    // notification ids
    public static final int MESSAGE_NOTIFICATION_ID = 1;
    public static final int SMS_NOTIFICATION_ID = 2;
    public static final int CALL_NOTIFICATION_ID = 3;

    private Context context;
    private static SenzNotificationManager instance;

    private SenzNotificationManager(Context context) {
        this.context = context;
    }

    public static SenzNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new SenzNotificationManager(context);
        }

        return instance;
    }

    /**
     * Display notification from here
     *
     * @param senzNotification
     */
    public void showNotification(SenzNotification senzNotification) {
        if (senzNotification.getNotificationType() == NotificationType.NEW_PERMISSION) {
            Notification notification = buildNotification(senzNotification);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(SenzNotificationManager.MESSAGE_NOTIFICATION_ID, notification);
        } else if (senzNotification.getNotificationType() == NotificationType.NEW_SECRET) {
            if (SenzApplication.isOnChat() && SenzApplication.getOnChatUser().equalsIgnoreCase(senzNotification.getSender())) {
                // message for currently chatting user
                Log.d(TAG, "Message for chatting user " + senzNotification.getSender());
            } else {
                // display other types of notification when user not on chat
                Notification notification = buildNotification(senzNotification);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(SenzNotificationManager.MESSAGE_NOTIFICATION_ID, notification);
            }
        } else if (senzNotification.getNotificationType() == NotificationType.SMS_REQUEST) {
            // SMS request
            Notification notification = buildSmsNotification(senzNotification);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(SenzNotificationManager.SMS_NOTIFICATION_ID, notification);
        }
    }

    /**
     * Get notification to create/ update
     * We need to create or update notification in different scenarios
     *
     * @return notification
     */
    private Notification buildNotification(SenzNotification senzNotification) {
        // set up pending intent
        Intent intent;
        if (senzNotification.getNotificationType() == NotificationType.NEW_SECRET) {
            intent = new Intent(context, ChatActivity.class);
            intent.putExtra("SENDER", senzNotification.getSender());
        } else {
            intent = new Intent(context, DrawerActivity.class);
            intent.putExtra("SENDER", senzNotification.getSender());
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(senzNotification.getTitle())
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX | Notification.DEFAULT_VIBRATE)
                .setContentText(senzNotification.getMessage())
                .setSmallIcon(senzNotification.getIcon())
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.eyes))
                .setLights(Color.GREEN, 500, 7000);

        return builder.build();
    }

    private Notification buildSmsNotification(SenzNotification senzNotification) {
        // accept
        Intent acceptIntent = new Intent();
        acceptIntent.setAction(IntentProvider.ACTION_SMS_REQUEST_ACCEPT);
        acceptIntent.putExtra("PHONE", senzNotification.getSenderPhone());
        acceptIntent.putExtra("USERNAME", senzNotification.getSender());
        acceptIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, 0, acceptIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Setup pending intent for dismiss action
        Intent cancelIntent = new Intent();
        cancelIntent.setAction(IntentProvider.ACTION_SMS_REQUEST_REJECT);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // content intent
        Intent contentIntent = new Intent(context, DrawerActivity.class);
        contentIntent.putExtra("SENDER", senzNotification.getSender());
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(senzNotification.getTitle())
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentText(senzNotification.getMessage())
                .setSmallIcon(senzNotification.getIcon())
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.accept, "Accept", acceptPendingIntent)
                .addAction(R.drawable.reject, "Reject", cancelPendingIntent)
                .setContentIntent(contentPendingIntent)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.eyes))
                .setLights(Color.GREEN, 500, 7000);

        return builder.build();
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
    void cancelNotification(int NotificationId, Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NotificationId);
    }

}
