package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AeroEditHistoryStore {
    private static final String PREFS_NAME = "aero_edit_history";
    private static final int MAX_VERSIONS_PER_MSG = 20;
    private static SharedPreferences prefs;
    private static final ConcurrentHashMap<String, ArrayList<EditEntry>> memoryCache = new ConcurrentHashMap<>();

    public static class EditEntry {
        public final int editDate;
        public final String text;

        public EditEntry(int editDate, String text) {
            this.editDate = editDate;
            this.text = text;
        }
    }

    public static void init(Context context) {
        if (prefs != null) {
            return;
        }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            Map<String, ?> all = prefs.getAll();
            if (all != null) {
                for (Map.Entry<String, ?> e : all.entrySet()) {
                    if (e.getValue() instanceof String) {
                        memoryCache.put(e.getKey(), parse((String) e.getValue()));
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    private static String key(int account, long dialogId, int msgId) {
        return account + "_" + dialogId + "_" + msgId;
    }

    private static ArrayList<EditEntry> parse(String json) {
        ArrayList<EditEntry> list = new ArrayList<>();
        if (TextUtils.isEmpty(json)) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    list.add(new EditEntry(obj.optInt("d", 0), obj.optString("t", "")));
                }
            }
        } catch (Exception ignore) {
        }
        return list;
    }

    private static String serialize(ArrayList<EditEntry> list) {
        JSONArray arr = new JSONArray();
        try {
            for (int i = 0; i < list.size(); i++) {
                EditEntry e = list.get(i);
                JSONObject obj = new JSONObject();
                obj.put("d", e.editDate);
                obj.put("t", e.text);
                arr.put(obj);
            }
        } catch (Exception ignore) {
        }
        return arr.toString();
    }

    public static synchronized boolean recordPreviousVersion(int account, long dialogId, int msgId, int editDate, String previousText) {
        if (!AeroConfig.saveEditHistory || dialogId == 0 || msgId <= 0 || previousText == null) {
            return false;
        }
        String trimmed = previousText.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String k = key(account, dialogId, msgId);
        ArrayList<EditEntry> list = memoryCache.get(k);
        if (list == null) {
            list = prefs != null ? parse(prefs.getString(k, "")) : new ArrayList<>();
            memoryCache.put(k, list);
        }
        if (!list.isEmpty()) {
            EditEntry last = list.get(list.size() - 1);
            if (TextUtils.equals(last.text, previousText)) {
                return false;
            }
        }
        list.add(new EditEntry(editDate, previousText));
        while (list.size() > MAX_VERSIONS_PER_MSG) {
            list.remove(0);
        }
        if (prefs != null) {
            prefs.edit().putString(k, serialize(list)).apply();
        }
        return true;
    }

    public static synchronized ArrayList<EditEntry> getHistory(int account, long dialogId, int msgId) {
        if (dialogId == 0 || msgId <= 0) {
            return new ArrayList<>();
        }
        String k = key(account, dialogId, msgId);
        ArrayList<EditEntry> list = memoryCache.get(k);
        if (list == null && prefs != null) {
            list = parse(prefs.getString(k, ""));
            memoryCache.put(k, list);
        }
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static synchronized boolean hasHistory(int account, long dialogId, int msgId) {
        if (dialogId == 0 || msgId <= 0) {
            return false;
        }
        String k = key(account, dialogId, msgId);
        ArrayList<EditEntry> list = memoryCache.get(k);
        if (list != null) {
            return !list.isEmpty();
        }
        return prefs != null && prefs.contains(k) && !parse(prefs.getString(k, "")).isEmpty();
    }

    public static synchronized int getTotalCount() {
        int count = 0;
        for (ArrayList<EditEntry> list : memoryCache.values()) {
            if (list != null) {
                count += list.size();
            }
        }
        return count;
    }

    public static synchronized void clearAll() {
        memoryCache.clear();
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}
