package com.score.chatz.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.score.chatz.application.IntentProvider;
import com.score.chatz.remote.SenzService;

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

                        String msg_body = msgs[i].getMessageBody();
                        if (isMessageFromRahasakApp(msg_body)) {
                            //Valid message
                            Log.e(TAG, "Valid message. From another rahasak user. -> " + msg_body);
                            Log.e(TAG, "Username to register. ->" + getUsernameFromSms(msg_body));

                            initAddUserFromSms(getUsernameFromSms(msg_body), context);
                        } else {
                            //Not from rahasak, ignore
                            Log.e(TAG, "Invalid message. Must ignore it. -> " + msg_body);
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

    private void initAddUserFromSms(String username, Context context) {
        Intent smsReceivedIntent = IntentProvider.getSmsReceivedIntent();
        smsReceivedIntent.putExtra("USERNAME", username);
        context.sendBroadcast(smsReceivedIntent);
    }
}