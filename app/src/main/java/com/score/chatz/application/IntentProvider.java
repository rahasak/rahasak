package com.score.chatz.application;

import android.content.IntentFilter;
import android.util.Log;

import com.score.chatz.enums.IntentType;
import com.score.chatz.exceptions.InvalidIntentType;

/**
 * This class is resposible to distrubute specific or general itents.
 * Please use this wrapper to send out intents inside the app
 */
public class IntentProvider {

    private static final String TAG = IntentProvider.class.getName();

    // intent actions
    public static final String ACTION_SENZ = "com.score.chatz.SENZ";
    public static final String ACTION_TIMEOUT = "com.score.chatz.TIMEOUT";
    public static final String ACTION_SMS_REQUEST_ACCEPT = "com.score.chatz.SMS_REQUEST_ACCEPT";
    public static final String ACTION_SMS_REQUEST_REJECT = "com.score.chatz.SMS_REQUEST_REJECT";
    public static final String ACTION_SMS_REQUEST_CONFIRM = "com.score.chatz.SMS_REQUEST_CONFIRM";
    public static final String ACTION_RESTART = "com.score.chatz.RESTART";
    public static final String ACTION_CONNECTED = "com.score.chatz.CONNECTED";

    /**
     * Return the intent filter for the intent_type.
     *
     * @param type intent type
     * @return
     */
    public static IntentFilter getIntentFilter(IntentType type) {
        try {
            return new IntentFilter(getIntentAction(type));
        } catch (InvalidIntentType ex) {
            Log.e(TAG, "No such intent, " + ex);
        }

        return null;
    }

    /**
     * Intent string generator
     * Get intents from this method, to centralize where intents are generated from for easier customization in the future.
     *
     * @param intentType intent type
     * @return
     */
    private static String getIntentAction(IntentType intentType) throws InvalidIntentType {
        switch (intentType) {
            case SENZ:
                return ACTION_SENZ;
            case TIMEOUT:
                return ACTION_TIMEOUT;
            case SMS_REQUEST_ACCEPT:
                return ACTION_SMS_REQUEST_ACCEPT;
            case SMS_REQUEST_REJECT:
                return ACTION_SMS_REQUEST_REJECT;
            case SMS_REQUEST_CONFIRM:
                return ACTION_SMS_REQUEST_CONFIRM;
            case CONNECTED:
                return ACTION_CONNECTED;
            default:
                throw new InvalidIntentType();
        }
    }

}
