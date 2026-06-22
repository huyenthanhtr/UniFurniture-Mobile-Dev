package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unifurniture.mobile.data.model.NotificationDto;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NotificationManager {

    private static final String PREFS_NAME = "unifurniture_notifications";
    private static final String KEY_NOTIFICATIONS = "notifications_list";
    private static final String KEY_SEEDED = "notifications_seeded";

    private static NotificationManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private NotificationManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }

    public synchronized List<NotificationDto> getNotifications() {
        String json = prefs.getString(KEY_NOTIFICATIONS, null);
        if (json == null) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply();
            return new ArrayList<>();
        }

        Type type = new TypeToken<ArrayList<NotificationDto>>() {}.getType();
        List<NotificationDto> list = gson.fromJson(json, type);
        if (list == null) {
            list = new ArrayList<>();
        }

        boolean changed = removeLegacySeedNotifications(list);
        if (changed) {
            saveNotifications(list);
        }

        Collections.sort(list, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
        return list;
    }

    public synchronized void addNotification(String title, String content, String type, String orderId) {
        List<NotificationDto> list = getNotifications();
        NotificationDto notification = new NotificationDto(
                UUID.randomUUID().toString(),
                title,
                content,
                type,
                System.currentTimeMillis(),
                false,
                orderId
        );
        list.add(0, notification);
        saveNotifications(list);
    }

    public synchronized void markAsRead(String id) {
        updateReadStatus(id, true);
    }

    public synchronized void markAsUnread(String id) {
        updateReadStatus(id, false);
    }

    private synchronized void updateReadStatus(String id, boolean isRead) {
        List<NotificationDto> list = getNotifications();
        boolean changed = false;
        for (NotificationDto n : list) {
            if (n.id.equals(id) && n.isRead != isRead) {
                n.isRead = isRead;
                changed = true;
                break;
            }
        }
        if (changed) {
            saveNotifications(list);
        }
    }

    public synchronized void markAsUnread(String id) {
        List<NotificationDto> list = getNotifications();
        boolean changed = false;
        for (NotificationDto n : list) {
            if (n.id.equals(id) && n.isRead) {
                n.isRead = false;
                changed = true;
                break;
            }
        }
        if (changed) {
            saveNotifications(list);
        }
    }

    public synchronized void markAllAsRead() {
        List<NotificationDto> list = getNotifications();
        boolean changed = false;
        for (NotificationDto n : list) {
            if (!n.isRead) {
                n.isRead = true;
                changed = true;
            }
        }
        if (changed) {
            saveNotifications(list);
        }
    }

    public synchronized boolean hasUnreadNotifications() {
        List<NotificationDto> list = getNotifications();
        for (NotificationDto n : list) {
            if (!n.isRead) {
                return true;
            }
        }
        return false;
    }

    private void saveNotifications(List<NotificationDto> list) {
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply();
    }

    private boolean removeLegacySeedNotifications(List<NotificationDto> list) {
        return list.removeIf(item -> {
            if (item == null || item.orderId == null) {
                return false;
            }
            return "UF-8824".equals(item.orderId) || "UF-8910".equals(item.orderId);
        });
    }
}
