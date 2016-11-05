package com.score.chatz.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by lakmalcaldera on 11/5/16.
 */

public class ContactsListAdapter extends SimpleCursorAdapter {

    private Typeface typeface;

    public ContactsListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    @Override
    public void setViewText(TextView v, String text) {
        super.setViewText(v, text);
        v.setTypeface(typeface);
    }
}
