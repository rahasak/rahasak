package com.score.chatz.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.score.chatz.utils.PhoneUtils;
import com.score.chatz.R;

/**
 * Created by lakmalcaldera on 11/5/16.
 */
class ContactsListAdapter extends SimpleCursorAdapter {

    private Typeface typeface;
    private Context _context;

    ContactsListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
        _context = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView contactName = (TextView) view.findViewById(R.id.contact_name);
        TextView contactNumber = (TextView) view.findViewById(R.id.contact_number);

        // Set up fonts
        contactName.setTypeface(typeface);
        contactNumber.setTypeface(typeface);

        // Extract properties from cursor
        String contactNameValue = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
        String contactNumbervalue = PhoneUtils.getFirstValidPhoneNumberFromContactId(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)), _context);

        // Populate fields with extracted properties
        contactName.setText(contactNameValue);
        contactNumber.setText(contactNumbervalue);
    }
}
