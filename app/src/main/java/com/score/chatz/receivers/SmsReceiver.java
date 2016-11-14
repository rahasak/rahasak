package com.score.chatz.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Permission;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.remote.SenzNotificationManager;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.PhoneUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lakmal on 11/2/16.
 */
public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = SmsReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                        String msg_phone_number = msgs[i].getOriginatingAddress();
                        String msg_sender = new PhoneUtils().getDisplayNameFromNumber(msg_phone_number, context);
                        String msg_body = msgs[i].getMessageBody();

                        if (isMessageFromRahasakApp(msg_body)) {
                            //Valid message
                            Log.e(TAG, "Valid message. From another rahasak user. -> " + msg_body + "; sender -> " + msg_sender);
                            Log.e(TAG, "Username to register. ->" + getUsernameFromSms(msg_body));

                            //Stop sms from reaching mail box :).Works for verision below kitkat only
                            abortBroadcast();

                            initAddUserFromSms(getUsernameFromSms(msg_body), msg_sender == null ? msg_phone_number : msg_sender, msg_phone_number, context);
                        } else {
                            //Not from rahasak, ignore
                            Log.e(TAG, "Invalid message. Must ignore it. -> " + msg_body + "; sender -> " + msg_sender);
                            return;
                        }
                    }
                } catch (Exception e) {
                    Log.d("Exception caught", e.getMessage());
                }
            }
        }
    }

    private boolean isMessageFromRahasakApp(String smsMessage) {
        return smsMessage.toLowerCase().contains("#rahasak");
    }

    private String getUsernameFromSms(String smsMessage) {
        final Pattern pattern = Pattern.compile("#username\\s(.*)$");
        final Matcher matcher = pattern.matcher(smsMessage);
        matcher.find();
        return matcher.group(1);
    }

    private void initAddUserFromSms(String username, String contactName, String contactPhone, Context context) {
        // chow Notification
        SenzNotificationManager.getInstance(context.getApplicationContext()).showNotification(NotificationUtils.getSmsNotification(contactName, contactPhone, username));

        SenzorsDbSource dbSource = new SenzorsDbSource(context);
        try {
            // create user
            SecretUser secretUser = new SecretUser("id", username);
            secretUser.setPhone(contactName);
            secretUser.setPhone(contactPhone);
            dbSource.createSecretUser(secretUser);

            // create permission
            dbSource.createPermission(new Permission("id", secretUser.getUsername(), true));
            dbSource.createPermission(new Permission("id", secretUser.getUsername(), false));

            // activate user
            dbSource.setSecretUserActive(secretUser.getUsername(), false);

            // Sent local intent to update view
            Intent intent = new Intent("com.score.chatz.SENZ");
            intent.putExtra("SMS_RECEIVED", "SMS_RECEIVED");
            context.sendBroadcast(intent);
        } catch (Exception ex) {
            // user exists
            ex.printStackTrace();
        }
    }
}