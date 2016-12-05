package com.score.chatz.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.score.chatz.utils.PhoneUtils;

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
    public void setViewText(TextView v, String text) {
        super.setViewText(v, text + " / " + PhoneUtils.getNumberFromName(text, _context));
        v.setTypeface(typeface);
    }
}
