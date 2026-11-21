package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class AeroConfig {
    private static final String PREFS_NAME = "aerogram_config";
    private static SharedPreferences prefs;

    // 1. Privacy & Ghost Mode (all off by default except saveDeleted & saveEditHistory like NovaGram)
    public static boolean ghostDontReadMessages = false;
    public static boolean ghostDontSendTyping = false;
    public static boolean ghostStayOffline = false;
    public static boolean ghostHideStoryViews = false;
    public static boolean ghostReadOnInteract = false;
    public static boolean saveDeletedMessages = true;
    public static boolean saveEditHistory = true;
    public static boolean screenshotProtection = false;
    public static boolean blockForeignNumbers = false;
    public static boolean confirmSendMedia = false;
    public static boolean confirmBeforeSendMedia = false;

    // Chat Lock & Hidden Chats
    public static boolean chatLockEnabled = false;
    public static String chatLockPin = "";
    public static boolean hiddenChatsUnlocked = false;
    public static final Set<Long> lockedDialogIds = new HashSet<>();
    public static final Set<Long> hiddenDialogIds = new HashSet<>();
    public static final Set<Long> tempUnlockedDialogIds = new HashSet<>();

    // 2. Translation & Voice-to-Text (STT)
    public static boolean autoTranslate = false;
    public static String translateTargetLang = "uz";
    public static boolean autoTranslateOutgoing = false;
    public static boolean askBeforeTranslateSend = false;
    public static boolean translateWholeChat = true;
    public static boolean voiceToTextEnabled = true;
    public static String voiceToTextLang = "auto";
    public static boolean autoTranslateVoice = false;

    // 3. Messages & Text Decorations
    public static final int STYLE_NONE = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_MONO = 3;
    public static final int STYLE_STRIKE = 4;
    public static final int STYLE_UNDERLINE = 5;
    public static final int STYLE_SPOILER = 6;

    public static int textStyleMode = STYLE_NONE;
    public static boolean newUzbekAlphabet = false;
    public static boolean autoPostfixEnabled = false;
    public static String autoPostfixText = "";
    public static boolean heartEffect = false;
    public static boolean autoSticker = false;
    public static boolean forwardWithoutAuthor = false;
    public static boolean forwardWithoutCaption = false;
    public static boolean confirmBeforeCall = false;
    public static boolean showIdAndDc = false;
    public static boolean showSecondsInTime = false;

    public static final int DOUBLE_TAP_REACTION = 0;
    public static final int DOUBLE_TAP_REPLY = 1;
    public static final int DOUBLE_TAP_COPY = 2;
    public static final int DOUBLE_TAP_TRANSLATE = 3;
    public static final int DOUBLE_TAP_NONE = 4;
    public static int doubleTapAction = DOUBLE_TAP_REACTION;

    // 4. Interface, Folders, Reminders & Media
    public static boolean hideFolders = false;
    public static final int FOLDER_STYLE_TEXT = 0;
    public static final int FOLDER_STYLE_ICON_TEXT = 1;
    public static final int FOLDER_STYLE_ICON = 2;
    public static int folderStyle = FOLDER_STYLE_TEXT;

    public static boolean showStoriesBar = true;
    public static boolean saveStoriesHd = true;
    public static boolean showGhostButtonInChats = true;
    public static boolean jumpToFirstMessageButton = false;
    public static boolean channelHistoryButton = false;
    public static boolean disableNumberRounding = false;
    public static boolean unlimitedPins = false;
    public static boolean showMutualBadge = false;
    public static boolean galleryRoundVideo = false;
    public static boolean askCameraType = false;
    public static boolean cameraSwitchOnHold = false;
    public static boolean preferBackCamera = false;
    public static boolean disableAutoDownload = false;
    public static boolean lightThemeBrandAccent = true;

    // 5. Proxy, Admin & Calls, UI Script
    public static boolean aeroProxyEnabled = false;
    public static boolean unreadReminderEnabled = false;
    public static int unreadReminderMinutes = 30;
    public static boolean unreadReminderSound = true;
    public static boolean autoAcceptJoinRequests = false;
    public static boolean adminFoldersEnabled = false;
    public static boolean recommendedFoldersEnabled = true;
    public static boolean recordCallsEnabled = false;
    public static boolean uiCyrillic = false;

    // Analytics & Stats cache
    public static int statBlockedForeignCount = 0;
    public static int cachedCommunityUsers = 0;
    public static long cachedCommunityFetchMs = 0L;

    public static void init(Context context) {
        if (prefs != null) {
            return;
        }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Migrate v1.1.1 installs where saveDeletedMessages/saveEditHistory defaulted to false
        if (!prefs.getBoolean("aero_v12_defaults", false)) {
            prefs.edit()
                .putBoolean("saveDeletedMessages", true)
                .putBoolean("saveEditHistory", true)
                .putBoolean("voiceToTextEnabled", true)
                .putBoolean("aero_v12_defaults", true)
                .apply();
        }

        ghostDontReadMessages = prefs.getBoolean("ghostDontReadMessages", false);
        ghostDontSendTyping = prefs.getBoolean("ghostDontSendTyping", false);
        ghostStayOffline = prefs.getBoolean("ghostStayOffline", false);
        ghostHideStoryViews = prefs.getBoolean("ghostHideStoryViews", false);
        ghostReadOnInteract = prefs.getBoolean("ghostReadOnInteract", false);
        saveDeletedMessages = prefs.getBoolean("saveDeletedMessages", true);
        saveEditHistory = prefs.getBoolean("saveEditHistory", true);
        screenshotProtection = prefs.getBoolean("screenshotProtection", false);
        blockForeignNumbers = prefs.getBoolean("blockForeignNumbers", false);
        confirmSendMedia = prefs.getBoolean("confirmSendMedia", false);
        confirmBeforeSendMedia = confirmSendMedia;

        chatLockEnabled = prefs.getBoolean("chatLockEnabled", false);
        chatLockPin = prefs.getString("chatLockPin", "");
        loadLongSet("lockedDialogIds", lockedDialogIds);
        loadLongSet("hiddenDialogIds", hiddenDialogIds);

        autoTranslate = prefs.getBoolean("autoTranslate", false);
        translateTargetLang = prefs.getString("translateTargetLang", "uz");
        autoTranslateOutgoing = prefs.getBoolean("autoTranslateOutgoing", false);
        askBeforeTranslateSend = prefs.getBoolean("askBeforeTranslateSend", false);
        translateWholeChat = prefs.getBoolean("translateWholeChat", true);
        voiceToTextEnabled = prefs.getBoolean("voiceToTextEnabled", true);
        voiceToTextLang = prefs.getString("voiceToTextLang", "auto");
        autoTranslateVoice = prefs.getBoolean("autoTranslateVoice", false);

        textStyleMode = prefs.getInt("textStyleMode", STYLE_NONE);
        newUzbekAlphabet = prefs.getBoolean("newUzbekAlphabet", false);
        autoPostfixEnabled = prefs.getBoolean("autoPostfixEnabled", false);
        autoPostfixText = prefs.getString("autoPostfixText", "");
        heartEffect = prefs.getBoolean("heartEffect", false);
        autoSticker = prefs.getBoolean("autoSticker", false);
        forwardWithoutAuthor = prefs.getBoolean("forwardWithoutAuthor", false);
        forwardWithoutCaption = prefs.getBoolean("forwardWithoutCaption", false);
        confirmBeforeCall = prefs.getBoolean("confirmBeforeCall", false);
        showIdAndDc = prefs.getBoolean("showIdAndDc", false);
        showSecondsInTime = prefs.getBoolean("showSecondsInTime", false);
        doubleTapAction = prefs.getInt("doubleTapAction", DOUBLE_TAP_REACTION);

        hideFolders = prefs.getBoolean("hideFolders", false);
        folderStyle = prefs.getInt("folderStyle", FOLDER_STYLE_TEXT);
        showStoriesBar = prefs.getBoolean("showStoriesBar", true);
        saveStoriesHd = prefs.getBoolean("saveStoriesHd", true);
        showGhostButtonInChats = prefs.getBoolean("showGhostButtonInChats", true);
        jumpToFirstMessageButton = prefs.getBoolean("jumpToFirstMessageButton", false);
        channelHistoryButton = prefs.getBoolean("channelHistoryButton", false);
        disableNumberRounding = prefs.getBoolean("disableNumberRounding", false);
        unlimitedPins = prefs.getBoolean("unlimitedPins", false);
        showMutualBadge = prefs.getBoolean("showMutualBadge", false);
        galleryRoundVideo = prefs.getBoolean("galleryRoundVideo", false);
        askCameraType = prefs.getBoolean("askCameraType", false);
        cameraSwitchOnHold = prefs.getBoolean("cameraSwitchOnHold", false);
        preferBackCamera = prefs.getBoolean("preferBackCamera", false);
        disableAutoDownload = prefs.getBoolean("disableAutoDownload", false);
        lightThemeBrandAccent = prefs.getBoolean("lightThemeBrandAccent", true);

        aeroProxyEnabled = prefs.getBoolean("aeroProxyEnabled", false);
        unreadReminderEnabled = prefs.getBoolean("unreadReminderEnabled", false);
        unreadReminderMinutes = prefs.getInt("unreadReminderMinutes", 30);
        unreadReminderSound = prefs.getBoolean("unreadReminderSound", true);
        autoAcceptJoinRequests = prefs.getBoolean("autoAcceptJoinRequests", false);
        adminFoldersEnabled = prefs.getBoolean("adminFoldersEnabled", false);
        recommendedFoldersEnabled = prefs.getBoolean("recommendedFoldersEnabled", true);
        recordCallsEnabled = prefs.getBoolean("recordCallsEnabled", false);
        uiCyrillic = prefs.getBoolean("uiCyrillic", false);

        statBlockedForeignCount = prefs.getInt("statBlockedForeignCount", 0);
        cachedCommunityUsers = prefs.getInt("cachedCommunityUsers", 0);
        cachedCommunityFetchMs = prefs.getLong("cachedCommunityFetchMs", 0L);
    }

    private static void loadLongSet(String key, Set<Long> target) {
        target.clear();
        if (prefs == null) return;
        Set<String> raw = prefs.getStringSet(key, null);
        if (raw == null) return;
        for (String s : raw) {
            try {
                target.add(Long.parseLong(s));
            } catch (Exception ignore) {
            }
        }
    }

    private static void saveLongSet(String key, Set<Long> source) {
        if (prefs == null) return;
        Set<String> raw = new HashSet<>(source.size());
        for (Long val : source) {
            raw.add(String.valueOf(val));
        }
        prefs.edit().putStringSet(key, raw).apply();
    }

    public static void saveBool(String key, boolean val) {
        if ("confirmSendMedia".equals(key)) {
            confirmBeforeSendMedia = val;
        }
        if (prefs != null) {
            prefs.edit().putBoolean(key, val).apply();
        }
    }

    public static void saveInt(String key, int val) {
        if (prefs != null) {
            prefs.edit().putInt(key, val).apply();
        }
    }

    public static void saveLong(String key, long val) {
        if (prefs != null) {
            prefs.edit().putLong(key, val).apply();
        }
    }

    public static void saveString(String key, String val) {
        if (prefs != null) {
            prefs.edit().putString(key, val == null ? "" : val).apply();
        }
    }

    public static boolean isAnyGhostActive() {
        return ghostDontReadMessages || ghostDontSendTyping || ghostStayOffline || ghostHideStoryViews;
    }

    public static void toggleMasterGhost(boolean enable) {
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

    public static void incrementBlockedForeign() {
        statBlockedForeignCount++;
        if (prefs != null) {
            prefs.edit().putInt("statBlockedForeignCount", statBlockedForeignCount).apply();
        }
    }

    public static boolean isDialogHidden(long dialogId) {
        return hiddenDialogIds.contains(dialogId);
    }

    public static void setDialogHidden(long dialogId, boolean hidden) {
        if (hidden) {
            hiddenDialogIds.add(dialogId);
        } else {
            hiddenDialogIds.remove(dialogId);
        }
        saveLongSet("hiddenDialogIds", hiddenDialogIds);
    }

    public static boolean isDialogLocked(long dialogId) {
        return chatLockEnabled && !TextUtils.isEmpty(chatLockPin) && lockedDialogIds.contains(dialogId) && !tempUnlockedDialogIds.contains(dialogId);
    }

    public static void setDialogLocked(long dialogId, boolean locked) {
        if (locked) {
            lockedDialogIds.add(dialogId);
        } else {
            lockedDialogIds.remove(dialogId);
            tempUnlockedDialogIds.remove(dialogId);
        }
        saveLongSet("lockedDialogIds", lockedDialogIds);
    }

    public static void unlockDialogForSession(long dialogId) {
        tempUnlockedDialogIds.add(dialogId);
    }

    public static String tr(String latin, String cyrillic) {
        return uiCyrillic ? cyrillic : latin;
    }

    public static void resetAll() {
        ghostDontReadMessages = false;
        ghostDontSendTyping = false;
        ghostStayOffline = false;
        ghostHideStoryViews = false;
        ghostReadOnInteract = false;
        saveDeletedMessages = true;
        saveEditHistory = true;
        screenshotProtection = false;
        blockForeignNumbers = false;
        confirmSendMedia = false;
        confirmBeforeSendMedia = false;

        chatLockEnabled = false;
        chatLockPin = "";
        hiddenChatsUnlocked = false;
        lockedDialogIds.clear();
        hiddenDialogIds.clear();
        tempUnlockedDialogIds.clear();

        autoTranslate = false;
        translateTargetLang = "uz";
        autoTranslateOutgoing = false;
        askBeforeTranslateSend = false;
        translateWholeChat = true;
        voiceToTextEnabled = true;
        voiceToTextLang = "auto";
        autoTranslateVoice = false;

        textStyleMode = STYLE_NONE;
        newUzbekAlphabet = false;
        autoPostfixEnabled = false;
        autoPostfixText = "";
        heartEffect = false;
        autoSticker = false;
        forwardWithoutAuthor = false;
        forwardWithoutCaption = false;
        confirmBeforeCall = false;
        showIdAndDc = false;
        showSecondsInTime = false;
        doubleTapAction = DOUBLE_TAP_REACTION;

        hideFolders = false;
        folderStyle = FOLDER_STYLE_TEXT;
        showStoriesBar = true;
        saveStoriesHd = true;
        showGhostButtonInChats = true;
        jumpToFirstMessageButton = false;
        channelHistoryButton = false;
        disableNumberRounding = false;
        unlimitedPins = false;
        showMutualBadge = false;
        galleryRoundVideo = false;
        askCameraType = false;
        cameraSwitchOnHold = false;
        preferBackCamera = false;
        disableAutoDownload = false;
        lightThemeBrandAccent = true;

        aeroProxyEnabled = false;
        unreadReminderEnabled = false;
        unreadReminderMinutes = 30;
        unreadReminderSound = true;
        autoAcceptJoinRequests = false;
        adminFoldersEnabled = false;
        recommendedFoldersEnabled = true;
        recordCallsEnabled = false;

        if (prefs != null) {
            boolean keepCyr = uiCyrillic;
            int users = cachedCommunityUsers;
            long fetchMs = cachedCommunityFetchMs;
            prefs.edit().clear()
                .putBoolean("aero_v12_defaults", true)
                .putBoolean("saveDeletedMessages", true)
                .putBoolean("saveEditHistory", true)
                .putBoolean("voiceToTextEnabled", true)
                .putBoolean("translateWholeChat", true)
                .putBoolean("showStoriesBar", true)
                .putBoolean("saveStoriesHd", true)
                .putBoolean("showGhostButtonInChats", true)
                .putBoolean("lightThemeBrandAccent", true)
                .putBoolean("recommendedFoldersEnabled", true)
                .putBoolean("unreadReminderSound", true)
                .putBoolean("uiCyrillic", keepCyr)
                .putInt("cachedCommunityUsers", users)
                .putLong("cachedCommunityFetchMs", fetchMs)
                .apply();
        }
    }
}
