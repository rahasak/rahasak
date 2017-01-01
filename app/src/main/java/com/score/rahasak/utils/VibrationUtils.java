package com.score.rahasak.utils;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by Lakmal on 9/6/16.
 */
public class VibrationUtils {

    public static long[] getVibratorPatterIncomingPhotoRequest() {
        return new long[]{0, 100, 1000, 300, 200, 100, 500, 200, 100};
    }

    public static void startVibrationForPhoto(long[] pattern, Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(pattern, 0);
    }

    public static void stopVibration(Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.cancel();
    }

    public static void vibrate(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

}
