package com.score.rahasak.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtils {
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a");
    private static SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd hh:mm a");

    public static String getTimeInWords(long timestamp) {
        String time = null;
        try {
            Calendar calendar = Calendar.getInstance();

            Date now = new Date();
            calendar.setTime(now);
            int nowDay = calendar.get(Calendar.DAY_OF_MONTH);

            Date pre = new Date(timestamp * 1000);
            calendar.setTime(pre);
            int preDay = calendar.get(Calendar.DAY_OF_MONTH);

            long deltaSeconds = now.getTime() / 1000 - timestamp;

            if (deltaSeconds < 10) {
                // just now
                time = "Now";
            } else if (deltaSeconds < 60) {
                // one minute ago
                time = "1 minute ago";
            } else if (deltaSeconds < (60 * 60)) {
                // few minutes ago
                time = deltaSeconds / 60 + " minutes ago";
            } else if (nowDay - preDay == 0) {
                // today
                time = TIME_FORMAT.format(new Date(timestamp * 1000));
            } else if (nowDay - preDay == 1) {
                // yesterday
                time = "Yesterday, " + TIME_FORMAT.format(new Date(timestamp * 1000));
            } else {
                // before yesterday
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
