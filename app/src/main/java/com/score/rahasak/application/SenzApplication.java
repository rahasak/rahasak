package com.score.rahasak.application;

import android.app.Application;

/**
 * Application class to hold shared attributes
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class SenzApplication extends Application {

    private static boolean onChat = false;

    private static String onChatUser = null;

    private static boolean onCall = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static boolean isOnChat() {
        return onChat;
    }

    public static void setOnChat(boolean onChat) {
        SenzApplication.onChat = onChat;
    }

    public static String getOnChatUser() {
        return onChatUser;
    }

    public static void setOnChatUser(String onChatUser) {
        SenzApplication.onChatUser = onChatUser;
    }

    public static boolean isOnCall() {
        return onCall;
    }

    public static void setOnCall(boolean onCall) {
        SenzApplication.onCall = onCall;
    }
}
