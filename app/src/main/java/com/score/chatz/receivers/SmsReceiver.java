package com.score.chatz.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;

import com.score.chatz.application.IntentProvider;
import com.score.chatz.remote.SenzNotificationManager;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.PhoneUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
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

                            initAddUserFromSms(getUsernameFromSms(msg_body), msg_sender == null ? msg_phone_number : msg_sender, msg_phone_number,context);
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

    private void initAddUserFromSms(String username, String sender, String phoneNumber,Context context) {
        SenzNotificationManager.getInstance(context.getApplicationContext()).showNotification(NotificationUtils.getSmsNotification(sender, phoneNumber,username));
    }

    private int deleteMessage(Context context) {
        Uri deleteUri = Uri.parse("content://sms");
        int count = 0;
        Cursor c = context.getContentResolver().query(deleteUri, null, null,
                null, null);
        while (c.moveToNext()) {
            try {
                // Delete the SMS
                String pid = c.getString(0); // Get id;
                String uri = "content://sms/" + pid;
                count = context.getContentResolver().delete(Uri.parse(uri),
                        null, null);
            } catch (Exception e) {
            }
        }
        return count;
    }
}