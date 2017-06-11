package com.score.rahasak.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a");
    private static SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd hh:mm a");

    public static String getTimeInWords(long timestamp) {
        String time = null;
        try {
            Date now = new Date();
            long deltaSeconds = now.getTime() / 1000 - timestamp;

            if (deltaSeconds < 10) {
                time = "now";
            } else if (deltaSeconds < 60) {
                time = "1 minute ago";
            } else if (deltaSeconds < (60 * 60)) {
                time = deltaSeconds / 60 + " minutes ago";
            } else if (deltaSeconds < (60 * 60 * 24)) {
                if (DATE_FORMAT.format(new Date(timestamp * 1000)).equalsIgnoreCase(DATE_FORMAT.format(now))) {
                    time = TIME_FORMAT.format(new Date(timestamp * 1000));
                } else {
                    time = DATE_TIME_FORMAT.format(new Date(timestamp * 1000));
                }
            } else {
                time = DATE_TIME_FORMAT.format(new Date(timestamp * 1000));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return time;
    }

    public static boolean isInOrder(long t1, long t2) {
        return (t2 - t1) < 60 * 5;
    }
}
