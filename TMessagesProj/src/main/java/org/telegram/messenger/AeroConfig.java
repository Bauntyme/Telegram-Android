package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public class AeroConfig {

    private static final String PREFS_NAME = "aerogram_config";
    private static SharedPreferences prefs;

    // Text style modes
    public static final int STYLE_NONE = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_MONO = 3;
    public static final int STYLE_STRIKE = 4;
    public static final int STYLE_UNDERLINE = 5;
    public static final int STYLE_SPOILER = 6;

    // Ghost Mode (all OFF by default)
    public static boolean ghostDontReadMessages = false;
    public static boolean ghostDontSendTyping = false;
    public static boolean ghostStayOffline = false;
    public static boolean ghostReadOnInteract = false;
    public static boolean ghostHideStoryViews = false;

    // Chat section
    public static boolean saveDeletedMessages = false;
    public static boolean saveEditHistory = false;
    public static boolean jumpToFirstMessageButton = false;
    public static boolean showGhostButtonInChats = false;
    public static boolean cameraSwitchOnHold = false;
    public static boolean preferBackCamera = false;
    public static boolean galleryRoundVideo = false;
    public static boolean forwardWithoutAuthor = false;
    public static boolean forwardWithoutCaption = false;

    // Interface section
    public static boolean hideFolders = false;
    public static boolean showStoriesBar = true;
    public static boolean showMutualBadge = false;
    public static boolean unlimitedPins = false;

    // Messages section
    public static int textStyleMode = STYLE_NONE;
    public static boolean autoPostfixEnabled = false;
    public static String autoPostfixText = "";
    public static boolean showTranslateButton = false;
    public static String translateTargetLang = "uz";
    public static boolean newUzbekAlphabet = false;
    public static boolean convertUzbekInList = false;
    public static boolean autoSticker = false;
    public static boolean heartEffect = false;

    // Security section
    public static boolean blockForeignNumbers = false;
    public static boolean disableAutoDownload = false;
    public static boolean confirmBeforeSendMedia = false;
    public static boolean screenshotProtection = false;

    // Functions & AeroGram Unique (2 custom features)
    public static boolean channelHistoryButton = false;
    public static boolean showSecondsInTime = false;
    public static boolean disableNumberRounding = false;
    public static boolean confirmBeforeCall = false;   // AeroGram Unique #1
    public static boolean showIdAndDc = false;         // AeroGram Unique #2

    // Local analytics counters
    public static int statBlockedForeignCount = 0;

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
        saveEditHistory = prefs.getBoolean("saveEditHistory", false);
        jumpToFirstMessageButton = prefs.getBoolean("jumpToFirstMessageButton", false);
        showGhostButtonInChats = prefs.getBoolean("showGhostButtonInChats", false);
        cameraSwitchOnHold = prefs.getBoolean("cameraSwitchOnHold", false);
        preferBackCamera = prefs.getBoolean("preferBackCamera", false);
        galleryRoundVideo = prefs.getBoolean("galleryRoundVideo", false);
        forwardWithoutAuthor = prefs.getBoolean("forwardWithoutAuthor", false);
        forwardWithoutCaption = prefs.getBoolean("forwardWithoutCaption", false);

        hideFolders = prefs.getBoolean("hideFolders", false);
        showStoriesBar = prefs.getBoolean("showStoriesBar", true);
        showMutualBadge = prefs.getBoolean("showMutualBadge", false);
        unlimitedPins = prefs.getBoolean("unlimitedPins", false);

        textStyleMode = prefs.getInt("textStyleMode", STYLE_NONE);
        autoPostfixEnabled = prefs.getBoolean("autoPostfixEnabled", false);
        autoPostfixText = prefs.getString("autoPostfixText", "");
        showTranslateButton = prefs.getBoolean("showTranslateButton", false);
        translateTargetLang = prefs.getString("translateTargetLang", "uz");
        newUzbekAlphabet = prefs.getBoolean("newUzbekAlphabet", false);
        convertUzbekInList = prefs.getBoolean("convertUzbekInList", false);
        autoSticker = prefs.getBoolean("autoSticker", false);
        heartEffect = prefs.getBoolean("heartEffect", false);

        blockForeignNumbers = prefs.getBoolean("blockForeignNumbers", false);
        disableAutoDownload = prefs.getBoolean("disableAutoDownload", false);
        confirmBeforeSendMedia = prefs.getBoolean("confirmBeforeSendMedia", false);
        screenshotProtection = prefs.getBoolean("screenshotProtection", false);

        channelHistoryButton = prefs.getBoolean("channelHistoryButton", false);
        showSecondsInTime = prefs.getBoolean("showSecondsInTime", false);
        disableNumberRounding = prefs.getBoolean("disableNumberRounding", false);
        confirmBeforeCall = prefs.getBoolean("confirmBeforeCall", false);
        showIdAndDc = prefs.getBoolean("showIdAndDc", false);

        statBlockedForeignCount = prefs.getInt("statBlockedForeignCount", 0);
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

    public static void saveInt(String key, int value) {
        ensureInit();
        if (prefs != null) {
            prefs.edit().putInt(key, value).apply();
        }
    }

    public static void saveString(String key, String value) {
        ensureInit();
        if (prefs != null) {
            prefs.edit().putString(key, value != null ? value : "").apply();
        }
    }

    public static void incrementBlockedForeign() {
        ensureInit();
        statBlockedForeignCount++;
        if (prefs != null) {
            prefs.edit().putInt("statBlockedForeignCount", statBlockedForeignCount).apply();
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

    public static String getTextStyleLabel() {
        switch (textStyleMode) {
            case STYLE_BOLD: return "Qalin (Bold)";
            case STYLE_ITALIC: return "Kursiv (Italic)";
            case STYLE_MONO: return "Kod (Monospace)";
            case STYLE_STRIKE: return "Ustiga chizilgan";
            case STYLE_UNDERLINE: return "Tagiga chizilgan";
            case STYLE_SPOILER: return "Yashirin (Spoiler)";
            default: return "Oddiy";
        }
    }

    public static String getTranslateLanguageLabel() {
        if ("en".equals(translateTargetLang)) return "English";
        if ("ru".equals(translateTargetLang)) return "Русский";
        if ("tr".equals(translateTargetLang)) return "Türkçe";
        if ("de".equals(translateTargetLang)) return "Deutsch";
        if ("fr".equals(translateTargetLang)) return "Français";
        if ("es".equals(translateTargetLang)) return "Español";
        if ("ar".equals(translateTargetLang)) return "العربية";
        return "O‘zbekcha";
    }

    public static void resetAll() {
        ensureInit();
        ghostDontReadMessages = false;
        ghostDontSendTyping = false;
        ghostStayOffline = false;
        ghostReadOnInteract = false;
        ghostHideStoryViews = false;

        saveDeletedMessages = false;
        saveEditHistory = false;
        jumpToFirstMessageButton = false;
        showGhostButtonInChats = false;
        cameraSwitchOnHold = false;
        preferBackCamera = false;
        galleryRoundVideo = false;
        forwardWithoutAuthor = false;
        forwardWithoutCaption = false;

        hideFolders = false;
        showStoriesBar = true;
        showMutualBadge = false;
        unlimitedPins = false;

        textStyleMode = STYLE_NONE;
        autoPostfixEnabled = false;
        autoPostfixText = "";
        showTranslateButton = false;
        translateTargetLang = "uz";
        newUzbekAlphabet = false;
        convertUzbekInList = false;
        autoSticker = false;
        heartEffect = false;

        blockForeignNumbers = false;
        disableAutoDownload = false;
        confirmBeforeSendMedia = false;
        screenshotProtection = false;

        channelHistoryButton = false;
        showSecondsInTime = false;
        disableNumberRounding = false;
        confirmBeforeCall = false;
        showIdAndDc = false;

        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}
