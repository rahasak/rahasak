package com.score.chatz.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.score.chatz.remote.SenzService;

/**
 * This receiver will be call
 * 1. on SenzService destroy
 * 2. on device boot
 * We have to start SenzService again from here
 */
public class SenzRestartReceiver extends BroadcastReceiver {

    private static final String TAG = SenzRestartReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Starting senz service");
        context.startService(new Intent(context, SenzService.class));
    }
}
