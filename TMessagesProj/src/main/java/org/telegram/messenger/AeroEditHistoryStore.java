package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AeroEditHistoryStore {

    public static class EditEntry {
        public final int date;
        public final String text;

        public EditEntry(int date, String text) {
            this.date = date;
            this.text = text;
        }
    }

    private static final String PREFS_NAME = "aerogram_edit_history";
    private static final int MAX_VERSIONS_PER_MSG = 15;
    private static final ConcurrentHashMap<String, List<EditEntry>> cache = new ConcurrentHashMap<>();
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
                if (val instanceof String) {
                    List<EditEntry> list = parseJson((String) val);
                    if (!list.isEmpty()) {
                        cache.put(entry.getKey(), list);
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

    private static String keyFor(int account, long dialogId, int msgId) {
        return account + "_" + dialogId + "_" + msgId;
    }

    private static List<EditEntry> parseJson(String raw) {
        List<EditEntry> list = Collections.synchronizedList(new ArrayList<>());
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    int date = obj.optInt("d", 0);
                    String text = obj.optString("t", "");
                    if (!TextUtils.isEmpty(text)) {
                        list.add(new EditEntry(date, text));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    private static String toJson(List<EditEntry> list) {
        JSONArray arr = new JSONArray();
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                EditEntry e = list.get(i);
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("d", e.date);
                    obj.put("t", e.text);
                    arr.put(obj);
                } catch (Throwable ignored) {
                }
            }
        }
        return arr.toString();
    }

    public static void recordPreviousVersion(int account, long dialogId, int msgId, int date, String previousText) {
        if (!AeroConfig.saveEditHistory || dialogId == 0 || msgId <= 0 || TextUtils.isEmpty(previousText)) {
            return;
        }
        ensureInit();
        String key = keyFor(account, dialogId, msgId);
        List<EditEntry> list = cache.get(key);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            List<EditEntry> prev = cache.putIfAbsent(key, list);
            if (prev != null) {
                list = prev;
            }
        }
        synchronized (list) {
            if (!list.isEmpty()) {
                EditEntry last = list.get(list.size() - 1);
                if (TextUtils.equals(last.text, previousText)) {
                    return;
                }
            }
            list.add(new EditEntry(date, previousText));
            while (list.size() > MAX_VERSIONS_PER_MSG) {
                list.remove(0);
            }
            if (prefs != null) {
                prefs.edit().putString(key, toJson(list)).apply();
            }
        }
    }

    public static boolean hasHistory(int account, long dialogId, int msgId) {
        ensureInit();
        List<EditEntry> list = cache.get(keyFor(account, dialogId, msgId));
        return list != null && !list.isEmpty();
    }

    public static ArrayList<EditEntry> getHistory(int account, long dialogId, int msgId) {
        ensureInit();
        List<EditEntry> list = cache.get(keyFor(account, dialogId, msgId));
        if (list == null) {
            return new ArrayList<>();
        }
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public static int getTotalCount() {
        ensureInit();
        int total = 0;
        for (List<EditEntry> list : cache.values()) {
            total += list.size();
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
