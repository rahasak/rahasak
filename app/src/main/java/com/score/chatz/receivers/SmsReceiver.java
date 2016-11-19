package com.score.chatz.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.score.chatz.db.SenzorsDbSource;
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
            SmsMessage smsMessage;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String format = bundle.getString("format");
                        smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0], format);
                    } else {
                        // this method was deprecated in API level 23.
                        smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0]);
                    }

                    if (isMessageFromRahasakApp(smsMessage.getMessageBody())) {
                        // valid message
                        if (isMessageConfirm(smsMessage.getMessageBody())) {
                            initFriendConfirmation(smsMessage, context);
                        } else if (isMessageRequest(smsMessage.getMessageBody())) {
                            initFriendRequest(smsMessage, context);
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

    private boolean isMessageConfirm(String smsMessage) {
        return smsMessage.toLowerCase().contains("#confirm");
    }

    private boolean isMessageRequest(String smsMessage) {
        return smsMessage.toLowerCase().contains("#request");
    }

    private String getUsernameFromSms(String smsMessage) {
        final Pattern pattern = Pattern.compile("#username\\s(\\S*)\\s");
        final Matcher matcher = pattern.matcher(smsMessage);
        matcher.find();
        return matcher.group(1);
    }

    private String getKeyHashFromSms(String smsMessage) {
        final Pattern pattern = Pattern.compile("#code\\s(.*)$");
        final Matcher matcher = pattern.matcher(smsMessage);
        matcher.find();
        return matcher.group(1);
    }

    private void initFriendRequest(SmsMessage smsMessage, Context context) {
        String contactNo = smsMessage.getOriginatingAddress();
        String contactName = PhoneUtils.getDisplayNameFromNumber(contactNo, context);
        String username = getUsernameFromSms(smsMessage.getMessageBody());
        String pubKeyHash = getKeyHashFromSms(smsMessage.getMessageBody());

        // chow Notification
        SenzNotificationManager.getInstance(context.getApplicationContext()).showNotification(NotificationUtils.getSmsNotification(contactName, contactNo, username));
        SenzorsDbSource dbSource = new SenzorsDbSource(context);
        try {
            // create user
            SecretUser secretUser = new SecretUser("id", username);
            secretUser.setPhone(contactNo);
            secretUser.setPubKeyHash(pubKeyHash);
            dbSource.createSecretUser(secretUser);

            // TODO broadcast
        } catch (Exception ex) {
            // user exists
            ex.printStackTrace();
        }
    }

    private void initFriendConfirmation(SmsMessage smsMessage, Context context) {
        String contactNo = smsMessage.getOriginatingAddress();
        String username = getUsernameFromSms(smsMessage.getMessageBody());
        String pubKeyHash = getKeyHashFromSms(smsMessage.getMessageBody());

        // create secret user
        SenzorsDbSource dbSource = new SenzorsDbSource(context);
        try {
            // create user
            SecretUser secretUser = new SecretUser("id", username);
            secretUser.setPhone(contactNo);
            secretUser.setPubKeyHash(pubKeyHash);
            secretUser.setSMSRequester(true);
            dbSource.createSecretUser(secretUser);

            // broadcast
            Intent intent = new Intent();
            intent.setAction("com.score.chatz.SMS_REQUEST_CONFIRM");
            intent.putExtra("USERNAME", username);
            intent.putExtra("PHONE", contactNo);
            context.sendBroadcast(intent);
        } catch (Exception ex) {
            // user exists
            ex.printStackTrace();
        }
    }
}