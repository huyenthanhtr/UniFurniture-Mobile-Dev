package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {

    private static final String PREF_NAME = "search_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_ITEMS = 10;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void add(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        List<String> list = getAll();
        list.remove(query);
        list.add(0, query);
        if (list.size() > MAX_ITEMS) list = list.subList(0, MAX_ITEMS);
        save(list);
        Log.d("SearchHistory", "add: " + query + " → list=" + list);
    }

    public List<String> getAll() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = gson.fromJson(json, type);
        List<String> result = list != null ? new ArrayList<>(list) : new ArrayList<>();
        Log.d("SearchHistory", "getAll: " + result);
        return result;
    }

    public void remove(String query) {
        List<String> list = getAll();
        list.remove(query);
        save(list);
    }

    public void clear() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private void save(List<String> list) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply();
    }
}
