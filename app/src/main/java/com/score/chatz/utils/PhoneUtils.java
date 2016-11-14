package com.score.chatz.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

/**
 * Created by Lakmal on 11/14/16.
 */

public class PhoneUtils {
    public String getPhoneNumber(Context context){
        TelephonyManager tMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getSimSerialNumber();
        return  mPhoneNumber;
    }

    public String getDisplayNameFromNumber(String phoneNumber, Context context){
        String displayName = phoneNumber;
        //Resolving the contact name from the contacts.
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor c = context.getContentResolver().query(lookupUri, new String[]{ContactsContract.Data.DISPLAY_NAME}, null, null, null);
        try {
            c.moveToFirst();
            displayName = c.getString(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return displayName;
    }
}
