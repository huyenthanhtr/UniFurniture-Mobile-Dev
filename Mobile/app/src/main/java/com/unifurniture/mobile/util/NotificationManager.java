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
            if (!prefs.getBoolean(KEY_SEEDED, false)) {
                return seedDefaultNotifications();
            }
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<NotificationDto>>() {}.getType();
        List<NotificationDto> list = gson.fromJson(json, type);
        if (list == null) {
            list = new ArrayList<>();
        }
        // Sort: newest first
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
        List<NotificationDto> list = getNotifications();
        boolean changed = false;
        for (NotificationDto n : list) {
            if (n.id.equals(id) && !n.isRead) {
                n.isRead = true;
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

    private List<NotificationDto> seedDefaultNotifications() {
        List<NotificationDto> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 1. Welcome - 3 days ago
        list.add(new NotificationDto(
                UUID.randomUUID().toString(),
                "Chào mừng đến với UniFurniture!",
                "Cảm ơn bạn đã lựa chọn UniFurniture. Hãy khám phá các bộ sưu tập bàn ghế và nội thất thông minh để bắt đầu trang trí tổ ấm của mình nhé!",
                "account",
                now - (3 * 24 * 60 * 60 * 1000L),
                false,
                null
        ));

        // 2. Member level up - 2 days ago
        list.add(new NotificationDto(
                UUID.randomUUID().toString(),
                "Tài khoản của bạn đã được nâng cấp",
                "Chúc mừng! Bạn đã trở thành thành viên Bạc của UniFurniture. Bạn nhận được ưu đãi giảm giá 5% tự động áp dụng cho các đơn hàng tiếp theo.",
                "account",
                now - (2 * 24 * 60 * 60 * 1000L),
                false,
                null
        ));

        // 3. Order shipped - 1 day ago
        list.add(new NotificationDto(
                UUID.randomUUID().toString(),
                "Đơn hàng #UF-8824 đã bàn giao vận chuyển",
                "Sản phẩm Sofa giường thông minh của bạn đang trên đường giao tới địa chỉ đăng ký. Tài xế sẽ liên hệ với bạn trước khi giao hàng 15-30 phút.",
                "order",
                now - (24 * 60 * 60 * 1000L),
                false,
                "UF-8824"
        ));

        // 4. Order success - 4 hours ago
        list.add(new NotificationDto(
                UUID.randomUUID().toString(),
                "Xác nhận đơn hàng #UF-8910 thành công",
                "Đơn hàng mua Bàn làm việc nâng hạ công thái học đã được hệ thống xác nhận. Đội ngũ kỹ thuật đang chuẩn bị sản phẩm để bàn giao cho đối tác giao nhận.",
                "order",
                now - (4 * 60 * 60 * 1000L),
                false,
                "UF-8910"
        ));

        saveNotifications(list);
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
        return list;
    }
}
