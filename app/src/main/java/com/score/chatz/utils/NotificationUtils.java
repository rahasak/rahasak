package com.score.chatz.utils;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import com.score.chatz.R;
import com.score.chatz.enums.NotificationType;
import com.score.chatz.pojo.SenzNotification;

import java.util.List;

/**
 * Utility class for create and update notifications
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class NotificationUtils {

    // notification Id
    public static final int SERVICE_NOTIFICATION_ID = 1;
    public static final int MESSAGE_NOTIFICATION_ID = 2;

    public static boolean isBackgroundRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String activeProcess : processInfo.pkgList) {
                    if (activeProcess.equals(context.getPackageName())) {
                        // App is now not in foreground!!!
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check if app is sleeping or screen lock is on!!!
     *
     * @param context
     * @return
     */
    public static boolean isAppInteractable(Context context) {
        //Acquire power manager
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        boolean isActive = false;

        //Check versions!!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if (pm.isInteractive()) {
                isActive = true;
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
            if (pm.isScreenOn()) {
                isActive = true;
            }
        }

        return isActive;
    }

    /**
     * Check if screen is locked or not!!
     *
     * @param context
     * @return
     */
    public static boolean isScreenLocked(Context context) {
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode()) {
            //it is locked
            return true;
        } else {
            //it is not locked
            return false;
        }
    }

    public static SenzNotification getNewUserNotification(String user) {
        String message = "You have been invited to share secrets.";
        return new SenzNotification(R.drawable.rahaslogo, "@" + user, message, user, NotificationType.NEW_USER);
    }

    public static SenzNotification getPermissionNotification(String user, String permissionName, String isEnabled) {
        if (permissionName.equalsIgnoreCase("lat")) {
            if (isEnabled.equalsIgnoreCase("on"))
                return new SenzNotification(R.drawable.rahaslogo, "@" + user, "You been granted location permission", user, NotificationType.NEW_PERMISSION);
            else
                return new SenzNotification(R.drawable.rahaslogo, "@" + user, "Your location permission has been revoked", user, NotificationType.NEW_PERMISSION);
        } else if (permissionName.equalsIgnoreCase("cam")) {
            if (isEnabled.equalsIgnoreCase("on"))
                return new SenzNotification(R.drawable.rahaslogo, "@" + user, "You been granted camera permission", user, NotificationType.NEW_PERMISSION);
            else
                return new SenzNotification(R.drawable.rahaslogo, "@" + user, "Your camera permission has been revoked", user, NotificationType.NEW_PERMISSION);
        } else if (permissionName.equalsIgnoreCase("mic")) {
            if (isEnabled.equalsIgnoreCase("on"))
                return new SenzNotification(R.drawable.rahaslogo, "@" + user, "You been granted mic permission", user, NotificationType.NEW_PERMISSION);
            else
                return new SenzNotification(R.drawable.rahaslogo, "@" + user, "Your mic permission has been revoked", user, NotificationType.NEW_PERMISSION);
        }

        return null;
    }

    public static SenzNotification getNewSecretNotification(String user, String message) {
        return new SenzNotification(R.drawable.rahaslogo, "@" + user, message, user, NotificationType.NEW_SECRET);
    }

}
