package com.score.chatz.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by lakmalcaldera on 11/5/16.
 */

public class ContactsListAdapter extends SimpleCursorAdapter {

    private Typeface typeface;

    public ContactsListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    public ContactsListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
    }

    @Override
    public ViewBinder getViewBinder() {
        return super.getViewBinder();
    }

    @Override
    public void setViewBinder(ViewBinder viewBinder) {
        super.setViewBinder(viewBinder);
    }

    @Override
    public void setViewImage(ImageView v, String value) {
        super.setViewImage(v, value);
    }

    @Override
    public void setViewText(TextView v, String text) {
        super.setViewText(v, text);
        v.setTypeface(typeface);
    }

    @Override
    public int getStringConversionColumn() {
        return super.getStringConversionColumn();
    }

    @Override
    public void setStringConversionColumn(int stringConversionColumn) {
        super.setStringConversionColumn(stringConversionColumn);
    }

    @Override
    public CursorToStringConverter getCursorToStringConverter() {
        return super.getCursorToStringConverter();
    }

    @Override
    public void setCursorToStringConverter(CursorToStringConverter cursorToStringConverter) {
        super.setCursorToStringConverter(cursorToStringConverter);
    }

    @Override
    public CharSequence convertToString(Cursor cursor) {
        return super.convertToString(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        return super.swapCursor(c);
    }

    @Override
    public void changeCursorAndColumns(Cursor c, String[] from, int[] to) {
        super.changeCursorAndColumns(c, from, to);
    }
}
