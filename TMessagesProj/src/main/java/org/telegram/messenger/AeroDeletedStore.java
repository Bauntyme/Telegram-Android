package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AeroDeletedStore {
    private static final String PREFS_NAME = "aero_deleted_messages";
    private static SharedPreferences prefs;
    private static final ConcurrentHashMap<String, Set<Integer>> cache = new ConcurrentHashMap<>();

    public static void init(Context context) {
        if (prefs != null) {
            return;
        }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            Map<String, ?> all = prefs.getAll();
            if (all != null) {
                for (Map.Entry<String, ?> entry : all.entrySet()) {
                    if (entry.getValue() instanceof Set<?>) {
                        Set<Integer> set = ConcurrentHashMap.newKeySet();
                        for (Object o : (Set<?>) entry.getValue()) {
                            try {
                                set.add(Integer.parseInt(String.valueOf(o)));
                            } catch (Exception ignore) {
                            }
                        }
                        cache.put(entry.getKey(), set);
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    private static String key(int account, long dialogId) {
        return account + "_" + dialogId;
    }

    public static synchronized void markDeleted(int account, long dialogId, int msgId) {
        if (dialogId == 0 || msgId <= 0) {
            return;
        }
        String k = key(account, dialogId);
        Set<Integer> set = cache.get(k);
        if (set == null) {
            set = ConcurrentHashMap.newKeySet();
            cache.put(k, set);
        }
        if (!set.add(msgId)) {
            return;
        }
        if (prefs != null) {
            Set<String> strSet = new HashSet<>(set.size());
            for (Integer id : set) {
                strSet.add(String.valueOf(id));
            }
            prefs.edit().putStringSet(k, strSet).apply();
        }
    }

    public static synchronized void markDeleted(int account, long dialogId, ArrayList<Integer> msgIds) {
        if (dialogId == 0 || msgIds == null || msgIds.isEmpty()) {
            return;
        }
        String k = key(account, dialogId);
        Set<Integer> set = cache.get(k);
        if (set == null) {
            set = ConcurrentHashMap.newKeySet();
            cache.put(k, set);
        }
        boolean changed = false;
        for (int i = 0; i < msgIds.size(); i++) {
            Integer id = msgIds.get(i);
            if (id != null && id > 0 && set.add(id)) {
                changed = true;
            }
        }
        if (changed && prefs != null) {
            Set<String> strSet = new HashSet<>(set.size());
            for (Integer id : set) {
                strSet.add(String.valueOf(id));
            }
            prefs.edit().putStringSet(k, strSet).apply();
        }
    }

    public static boolean isDeleted(int account, long dialogId, int msgId) {
        if (dialogId == 0 || msgId <= 0) {
            return false;
        }
        Set<Integer> set = cache.get(key(account, dialogId));
        return set != null && set.contains(msgId);
    }

    public static synchronized int getTotalSavedCount() {
        int count = 0;
        for (Set<Integer> set : cache.values()) {
            count += set.size();
        }
        return count;
    }

    public static synchronized void clearAll() {
        cache.clear();
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}
