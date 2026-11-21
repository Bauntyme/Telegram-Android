package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public class AeroConfig {

    private static final String PREFS_NAME = "aerogram_config";
    private static SharedPreferences prefs;

    // Ghost Mode (all OFF by default)
    public static boolean ghostDontReadMessages = false;
    public static boolean ghostDontSendTyping = false;
    public static boolean ghostStayOffline = false;
    public static boolean ghostReadOnInteract = false;
    public static boolean ghostHideStoryViews = false;

    // Anti-Delete single switch (OFF by default; covers private chats, groups & channels)
    public static boolean saveDeletedMessages = false;

    // Chats & Appearance tweaks (all OFF by default)
    public static boolean forwardWithoutAuthor = false;
    public static boolean forwardWithoutCaption = false;
    public static boolean showSecondsInTime = false;
    public static boolean disableNumberRounding = false;

    // Security (optional extra protection OFF by default)
    public static boolean screenshotProtection = false;

    private static volatile boolean loaded = false;

    public static synchronized void init(Context context) {
        if (loaded || context == null) {
            return;
        }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ghostDontReadMessages = prefs.getBoolean("ghostDontReadMessages", false);
        ghostDontSendTyping = prefs.getBoolean("ghostDontSendTyping", false);
        ghostStayOffline = prefs.getBoolean("ghostStayOffline", false);
        ghostReadOnInteract = prefs.getBoolean("ghostReadOnInteract", false);
        ghostHideStoryViews = prefs.getBoolean("ghostHideStoryViews", false);

        saveDeletedMessages = prefs.getBoolean("saveDeletedMessages", false);

        forwardWithoutAuthor = prefs.getBoolean("forwardWithoutAuthor", false);
        forwardWithoutCaption = prefs.getBoolean("forwardWithoutCaption", false);
        showSecondsInTime = prefs.getBoolean("showSecondsInTime", false);
        disableNumberRounding = prefs.getBoolean("disableNumberRounding", false);

        screenshotProtection = prefs.getBoolean("screenshotProtection", false);
        loaded = true;
    }

    private static void ensureInit() {
        if (!loaded && ApplicationLoader.applicationContext != null) {
            init(ApplicationLoader.applicationContext);
        }
    }

    public static void saveBool(String key, boolean value) {
        ensureInit();
        if (prefs != null) {
            prefs.edit().putBoolean(key, value).apply();
        }
    }

    public static boolean isAnyGhostActive() {
        return ghostDontReadMessages || ghostDontSendTyping || ghostStayOffline || ghostHideStoryViews;
    }

    public static void toggleMasterGhost(boolean enable) {
        ensureInit();
        ghostDontReadMessages = enable;
        ghostDontSendTyping = enable;
        ghostStayOffline = enable;
        ghostHideStoryViews = enable;
        if (prefs != null) {
            prefs.edit()
                .putBoolean("ghostDontReadMessages", enable)
                .putBoolean("ghostDontSendTyping", enable)
                .putBoolean("ghostStayOffline", enable)
                .putBoolean("ghostHideStoryViews", enable)
                .apply();
        }
    }

    public static void resetAll() {
        ensureInit();
        ghostDontReadMessages = false;
        ghostDontSendTyping = false;
        ghostStayOffline = false;
        ghostReadOnInteract = false;
        ghostHideStoryViews = false;
        saveDeletedMessages = false;
        forwardWithoutAuthor = false;
        forwardWithoutCaption = false;
        showSecondsInTime = false;
        disableNumberRounding = false;
        screenshotProtection = false;
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}
