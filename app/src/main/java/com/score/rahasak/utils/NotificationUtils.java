package com.score.rahasak.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.score.rahasak.R;
import com.score.rahasak.enums.NotificationType;
import com.score.rahasak.pojo.SenzNotification;

/**
 * Utility class for create and update notifications
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class NotificationUtils {


    public static SenzNotification getPermissionNotification(String user, String permissionName, String isEnabled) {
        String msg;
        if (isEnabled.equalsIgnoreCase("on")) {
            msg = "You been granted " + permissionName + " permission";
        } else {
            msg = "Your " + permissionName + " permission has been revoked";
        }

        return new SenzNotification(R.drawable.ic_notification, user, msg, user, NotificationType.NEW_PERMISSION);
    }

    public static SenzNotification getUserNotification(String user) {
        return new SenzNotification(R.drawable.ic_notification, user, "You have been invited to share secrets", user, NotificationType.NEW_PERMISSION);
    }

    public static SenzNotification getUserConfirmNotification(String user) {
        return new SenzNotification(R.drawable.ic_notification, user, "Confirmed your secret request", user, NotificationType.NEW_PERMISSION);
    }

    public static SenzNotification getSecretNotification(String title, String user, String message) {
        return new SenzNotification(R.drawable.ic_notification, title, message, user, NotificationType.NEW_SECRET);
    }

    public static SenzNotification getStreamNotification(String title, String message, String user) {
        return new SenzNotification(R.drawable.ic_notification, title, message, user, NotificationType.NEW_SECRET);
    }

    public static SenzNotification getSmsNotification(String contactName, String contactPhone, String rahasakUsername) {
        String msg = "Would you like share secrets?";

        SenzNotification senzNotification = new SenzNotification(R.drawable.ic_notification, contactName, msg, rahasakUsername, NotificationType.SMS_REQUEST);
        senzNotification.setSenderPhone(contactPhone);

        return senzNotification;
    }

    public static Notification getCallNotification(Context context, String title, String message, PendingIntent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(intent)
                .setOngoing(true);

        return builder.build();
    }

}
