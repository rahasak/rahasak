package com.score.chatz.utils;

import android.app.NotificationManager;
import android.content.Context;

import com.score.chatz.R;
import com.score.chatz.enums.NotificationType;
import com.score.chatz.pojo.SenzNotification;

/**
 * Utility class for create and update notifications
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class NotificationUtils {

    // notification Id
    public static final int MESSAGE_NOTIFICATION_ID = 1;
    public static final int SMS_REQUEST_NOTIFICATION_ID = 2;
    public static final int SMS_CONFIRM_NOTIFICATION_ID = 3;

    public static SenzNotification getPermissionNotification(String user, String permissionName, String isEnabled) {
        String msg;
        if (isEnabled.equalsIgnoreCase("on")) {
            msg = "You been granted " + permissionName + " permission";
        } else {
            msg = "Your " + permissionName + " permission has been revoked";
        }

        return new SenzNotification(R.drawable.notification_icon, "@" + user, msg, user, NotificationType.NEW_PERMISSION);
    }

    public static SenzNotification getUserNotification(String user) {
        return new SenzNotification(R.drawable.notification_icon, "@" + user, "You have been invited to share secrets", user, NotificationType.NEW_PERMISSION);
    }

    public static SenzNotification getSecretNotification(String user, String message) {
        return new SenzNotification(R.drawable.notification_icon, "@" + user, message, user, NotificationType.NEW_SECRET);
    }

    public static SenzNotification getStreamNotification(String user, boolean isCam) {
        String msg;
        if (isCam) {
            msg = "New selfie secret received";
        } else {
            msg = "New voice secret received";
        }

        return new SenzNotification(R.drawable.notification_icon, "@" + user, msg, user, NotificationType.NEW_SECRET);
    }

    public static SenzNotification getSmsNotification(String contactName, String contactPhone, String rahasakUsername) {
        String msg = "Would you like share secrets?";
        String title = contactName + " (@" + rahasakUsername + ")";

        SenzNotification senzNotification = new SenzNotification(R.drawable.notification_icon, title, msg, rahasakUsername, NotificationType.SMS_REQUEST);
        senzNotification.setSenderPhone(contactPhone);

        return senzNotification;
    }

    // Remove notification from tray
    public static void cancelNotification(int NotificationId, Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NotificationId);
    }

}
