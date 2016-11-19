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

    public static Intent getTimeoutIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.TIMEOUT));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }
        return intent;
    }

    public static Intent getAddUserIntent() {
        Intent intent = null;
        try {
            intent = getIntent(getIntentType(INTENT_TYPE.ADD_USER));
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
        switch (intentType) {
            case SENZ:
                return "com.score.chatz.SENZ";
            case TIMEOUT:
                // depressing!! That #$%! is not online!! :)
                return "com.score.chatz.TIMEOUT";
            case ADD_USER:
                return "com.score.chatz.ADD_USER";
            case SMS_REQUEST_ACCEPT:
                return "com.score.chatz.SMS_REQUEST_ACCEPT";
            case SMS_REQUEST_REJECT:
                return "com.score.chatz.SMS_REQUEST_REJECT";
            case SMS_REQUEST_CONFIRM:
                return "com.score.chatz.SMS_REQUEST_CONFIRM";
            default:
                throw new InvalidIntentType();
        }
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
        SENZ, TIMEOUT, ADD_USER, SMS_REQUEST_ACCEPT, SMS_REQUEST_REJECT, SMS_REQUEST_CONFIRM
    }
}
