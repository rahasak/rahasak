package com.score.chatz.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;

/**
 * Created by Lakmal on 11/14/16.
 */

public class PhoneUtils {
    public static String getDisplayNameFromNumber(String phoneNumber, Context context) {
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

    public static String getFirstValidPhoneNumberFromContactId(String contactId, Context context) {
        ArrayList<String> phoneNum = new ArrayList<String>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { contactId + "" }, null);
        if (cursor.getCount() >= 1) {
            while (cursor.moveToNext()) {
                // store the numbers in an array
                String str = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (str != null && str.trim().length() > 0) {
                    phoneNum.add(str);
                }
            }
        }
        cursor.close();
        return phoneNum.get(0);
    }
}
