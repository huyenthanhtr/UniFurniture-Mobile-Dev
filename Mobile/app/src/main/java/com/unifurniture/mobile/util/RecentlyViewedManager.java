package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecentlyViewedManager {

    private static final String PREFS = "recently_viewed";
    private static final String KEY = "items";
    private static final int MAX = 10;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public static class Item {
        public String id;
        public String slug;
        public String name;
        public String imageUrl;
        // Language the cached `name` is in; lets the UI refetch when the app language changes.
        public String lang;

        public Item(String id, String slug, String name, String imageUrl) {
            this.id = id;
            this.slug = slug;
            this.name = name;
            this.imageUrl = imageUrl;
        }
    }

    public RecentlyViewedManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void add(Item item) {
        if (item == null || item.id == null) return;
        item.lang = LanguageHelper.getLanguage(appContext); // stamp the language of this name
        List<Item> list = getAll();
        list.removeIf(i -> item.id.equals(i.id));
        list.add(0, item);
        if (list.size() > MAX) list = list.subList(0, MAX);
        prefs.edit().putString(KEY, gson.toJson(list)).apply();
    }

    /** Persist an updated list (e.g. after refetching names in a new language). */
    public void saveAll(List<Item> list) {
        if (list == null) return;
        if (list.size() > MAX) list = list.subList(0, MAX);
        prefs.edit().putString(KEY, gson.toJson(list)).apply();
    }

    public List<Item> getAll() {
        String json = prefs.getString(KEY, "[]");
        Type type = new TypeToken<List<Item>>() {}.getType();
        List<Item> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }
}
