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

                        String msg_sender = null;
                        String msg_phone_number = msgs[i].getOriginatingAddress();
                        String msg_body = msgs[i].getMessageBody();

                        //Resolving the contact name from the contacts.
                        Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(msg_phone_number));
                        Cursor c = context.getContentResolver().query(lookupUri, new String[]{ContactsContract.Data.DISPLAY_NAME}, null, null, null);
                        try {
                            c.moveToFirst();
                            msg_sender = c.getString(0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            c.close();
                        }

                        if (isMessageFromRahasakApp(msg_body)) {
                            //Valid message
                            Log.e(TAG, "Valid message. From another rahasak user. -> " + msg_body + "; sender -> " + msg_sender);
                            Log.e(TAG, "Username to register. ->" + getUsernameFromSms(msg_body));

                            //Stop sms from reaching mail box :).Works for verision below kitkat only
                            abortBroadcast();

                            initAddUserFromSms(getUsernameFromSms(msg_body), msg_sender == null ? msg_phone_number : msg_sender, context);
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

    private void initAddUserFromSms(String username, String sender, Context context) {
        Intent smsReceivedIntent = IntentProvider.getSmsReceivedIntent();
        smsReceivedIntent.putExtra("USERNAME_TO_ADD", username);
        smsReceivedIntent.putExtra("SENDER", sender);
        context.sendBroadcast(smsReceivedIntent);
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