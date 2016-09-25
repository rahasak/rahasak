package com.score.chatz.application;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.score.chatz.exceptions.InvalidIntentType;
import com.score.chatz.ui.PhotoActivity;
import com.score.chatz.ui.RecordingActivity;

/**
 * This class is resposible to distrubute specific or general itents.
 * Please use this wrapper to send out intents inside the app
 * <p>
 * Note:- TODO integrate all intents into this wrapper.
 * <p>
 * Created by Lakmal on 9/4/16.
 */
public class IntentProvider {
    private static final String TAG = IntentProvider.class.getName();

    public static Intent getDataSenzIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.DATA_SENZ));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    public static Intent getShareSenzIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.SHARE_SENZ));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    public static Intent getUserBusyIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.USER_BUSY));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    public static Intent getpacketTimeoutIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.PACKET_TIMEOUT));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    public static Intent getNewDataToDisplayIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.NEW_DATA_TO_DISPLAY));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    public static Intent getNoLocationEnabledIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.NO_LOC_ENABLED));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    /**
     * Return the intent filter for the intent_type.
     *
     * @param type
     * @return
     */
    public static IntentFilter getIntentFilter(INTENT_TYPE type) {
        IntentFilter intentFilter = null;
        try {
            intentFilter = new IntentFilter(getIntentType(type));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intentFilter;
    }

    public static Intent getCameraIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, PhotoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent getRecorderIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, RecordingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return intent;
    }

    /**
     * Intent string generator
     * Get intents from this method, to centralize where intents are generated from for easier customization in the future.
     *
     * @param intentType
     * @return
     */
    private static String getIntentType(INTENT_TYPE intentType) throws InvalidIntentType {
        String intentString = null;
        switch (intentType) {
            case DATA_SENZ:
                // Yummy!! Data packets from service
                intentString = "com.score.chatz.DATA_SENZ";
                break;
            case SHARE_SENZ:
                // share senz
                intentString = "com.score.chatz.SHARE_SENZ";
                break;
            case USER_BUSY:
                // Ohhh!! User is too busy to respond
                intentString = "com.score.chatz.USER_BUSY";
                break;
            case PACKET_TIMEOUT:
                // Depressing!! That #$%! is not online!! :)
                intentString = "com.score.chatz.PACKET_TIMEOUT";
                break;
            case NEW_DATA_TO_DISPLAY:
                intentString = "com.score.chatz.NEW_DATA_TO_DISPLAY";
                break;
            case NO_LOC_ENABLED:
                intentString = "com.score.chatz.NO_LOC_ENABLED";
                break;
            default:
                throw new InvalidIntentType();
        }
        return intentString;
    }

    /**
     * return an intent object for your intentString
     *
     * @param intentString
     * @return
     */
    private static Intent getIntent(String intentString) {
        return new Intent(intentString);
    }

    public enum INTENT_TYPE {
        DATA_SENZ, SHARE_SENZ, USER_BUSY, PACKET_TIMEOUT, NEW_DATA_TO_DISPLAY, NO_LOC_ENABLED
    }
}
