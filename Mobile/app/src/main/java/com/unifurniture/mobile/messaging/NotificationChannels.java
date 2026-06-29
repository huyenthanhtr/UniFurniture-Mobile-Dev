package com.unifurniture.mobile.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.unifurniture.mobile.R;

/** Defines and creates the app's notification channels (API 26+). */
public final class NotificationChannels {

    public static final String ORDERS = "orders";
    public static final String PROMOTIONS = "promotions";
    public static final String GENERAL = "general"; // must match @string/fcm_default_channel_id

    private NotificationChannels() {}

    public static void createAll(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(
                ORDERS, ctx.getString(R.string.channel_orders_name), NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(
                PROMOTIONS, ctx.getString(R.string.channel_promotions_name), NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(
                GENERAL, ctx.getString(R.string.channel_general_name), NotificationManager.IMPORTANCE_DEFAULT));
    }

    /** Map an FCM message {@code type} to a channel id. */
    public static String channelForType(String type) {
        if (type == null) return GENERAL;
        switch (type) {
            case "order":
                return ORDERS;
            case "promotion":
            case "recommendation":
                return PROMOTIONS;
            default:
                return GENERAL;
        }
    }
}
