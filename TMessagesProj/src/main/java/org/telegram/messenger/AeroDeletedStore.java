package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AeroDeletedStore {

    private static final String PREFS_NAME = "aerogram_deleted_messages";
    private static final ConcurrentHashMap<String, Set<Integer>> cache = new ConcurrentHashMap<>();
    private static SharedPreferences prefs;
    private static volatile boolean loaded = false;

    public static synchronized void init(Context context) {
        if (loaded || context == null) {
            return;
        }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        if (all != null) {
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Set<?>) {
                    Set<Integer> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    for (Object item : (Set<?>) val) {
                        if (item instanceof String) {
                            try {
                                ids.add(Integer.parseInt((String) item));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    if (!ids.isEmpty()) {
                        cache.put(entry.getKey(), ids);
                    }
                }
            }
        }
        loaded = true;
    }

    private static void ensureInit() {
        if (!loaded && ApplicationLoader.applicationContext != null) {
            init(ApplicationLoader.applicationContext);
        }
    }

    private static String keyFor(int account, long dialogId) {
        return account + "_" + dialogId;
    }

    public static void markDeleted(int account, long dialogId, int msgId) {
        ensureInit();
        String key = keyFor(account, dialogId);
        Set<Integer> set = cache.get(key);
        if (set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Set<Integer> prev = cache.putIfAbsent(key, set);
            if (prev != null) {
                set = prev;
            }
        }
        if (set.add(msgId)) {
            persistKey(key, set);
        }
    }

    public static void markDeleted(int account, long dialogId, ArrayList<Integer> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return;
        }
        ensureInit();
        String key = keyFor(account, dialogId);
        Set<Integer> set = cache.get(key);
        if (set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Set<Integer> prev = cache.putIfAbsent(key, set);
            if (prev != null) {
                set = prev;
            }
        }
        boolean changed = false;
        for (int i = 0; i < msgIds.size(); i++) {
            Integer id = msgIds.get(i);
            if (id != null && set.add(id)) {
                changed = true;
            }
        }
        if (changed) {
            persistKey(key, set);
        }
    }

    private static void persistKey(String key, Set<Integer> set) {
        if (prefs == null) {
            return;
        }
        Set<String> stringSet = new HashSet<>(set.size());
        for (Integer id : set) {
            stringSet.add(String.valueOf(id));
        }
        prefs.edit().putStringSet(key, stringSet).apply();
    }

    public static boolean isDeleted(int account, long dialogId, int msgId) {
        ensureInit();
        Set<Integer> direct = cache.get(keyFor(account, dialogId));
        if (direct != null && direct.contains(msgId)) {
            return true;
        }
        if (dialogId >= 0L) {
            Set<Integer> mailbox = cache.get(keyFor(account, 0L));
            if (mailbox != null && mailbox.contains(msgId)) {
                return true;
            }
        }
        return false;
    }

    public static int getTotalSavedCount() {
        ensureInit();
        int total = 0;
        for (Set<Integer> set : cache.values()) {
            total += set.size();
        }
        return total;
    }

    public static void clearAll() {
        ensureInit();
        cache.clear();
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}
