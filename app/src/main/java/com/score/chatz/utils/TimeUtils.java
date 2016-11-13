package com.score.chatz.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lakmal on 9/5/16.
 */
public class TimeUtils {
    private static final String TAG = TimeUtils.class.getName();

    public static SimpleDateFormat getDateSimpleDateFormat() {
        return new SimpleDateFormat("MM/dd/yyyy");
    }

    public static SimpleDateFormat getTimeSimpleDateFormat() {
        return new SimpleDateFormat("HH:mm a");
    }

    public static SimpleDateFormat getDateAndTimeSimpleDateFormat() {
        return new SimpleDateFormat("MM/dd/yyyy' 'HH:mm a");
    }

    public static String getTimeInWords(long timestamp) {
        String time = null;
        try {
            Date now = new Date();
            long deltaSeconds = now.getTime() / 1000 - timestamp;

            if (deltaSeconds < 3) {
                time = "now";
            } else if (deltaSeconds < 5) {
                time = "less than 5 seconds ago";
            } else if (deltaSeconds < 10) {
                time = "less than 10 seconds ago";
            } else if (deltaSeconds < 60) {
                time = "less than a minute ago";
            } else if (deltaSeconds < (60 * 60)) {
                time = deltaSeconds / 60 + " minutes ago";//getSimpleDateFormatForChatMessages().format(past);
            } else if (deltaSeconds < (60 * 60 * 24)) {
                time = deltaSeconds / (60 * 60) + " hours ago";
            } else if (deltaSeconds < (60 * 60 * 24 * 2)) {
                time = deltaSeconds / (60 * 60 * 24) + " days ago";
            } else {
                time = getDateAndTimeSimpleDateFormat().format(new Date(timestamp * 1000));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return time;
    }

    /**
     * Return true is the date supplied is older than timeInMinues.
     *
     * @param date
     * @param timeInMinutes
     * @return
     */
    public static Boolean isDatePast(Date date, Integer timeInMinutes) {
        Boolean isPast = false;
        try {
            Date now = new Date();
            long deltaSeconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - date.getTime());
            if (deltaSeconds > (timeInMinutes * 60)) {
                isPast = true;
            } else {
                isPast = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isPast;
    }
}
