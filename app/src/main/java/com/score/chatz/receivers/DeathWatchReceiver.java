package com.score.chatz.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.score.chatz.remote.SenzService;

/**
 * Created by eranga on 8/19/16.
 */
public class DeathWatchReceiver extends BroadcastReceiver {

    private static final String TAG = BootCompleteReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "death watch");
        Log.d(TAG, "Starting senz service");
        context.startService(new Intent(context, SenzService.class));
    }
}
