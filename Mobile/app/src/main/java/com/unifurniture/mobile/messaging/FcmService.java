package com.unifurniture.mobile.messaging;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.ui.MainActivity;

import java.util.Map;

/**
 * Receives FCM pushes. Each message is shown as a system notification on the channel mapped from its
 * {@code type}. The in-app list is NOT written here: the backend already persists every notification
 * (sendToCustomer) and the app syncs it from {@code /api/notifications}, so a local copy would be a
 * duplicate (local UUID vs server _id never merge).
 *
 * Expected data payload keys: {@code type} (order|promotion|review|account|recommendation),
 * {@code title}, {@code body}, optional {@code orderId}, {@code couponCode}, {@code deepLink}.
 */
public class FcmService extends FirebaseMessagingService {

    public static final String EXTRA_ORDER_ID = "nav_order_id";
    public static final String EXTRA_DEEP_LINK = "nav_deep_link";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> data = message.getData();

        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");
        if (message.getNotification() != null) {
            if (title == null) title = message.getNotification().getTitle();
            if (body == null) body = message.getNotification().getBody();
        }
        if (title == null) title = getString(R.string.app_name);
        if (body == null) body = "";
        String orderId = data.get("orderId");
        String deepLink = data.get("deepLink");

        // The in-app list is server-sourced (synced via /api/notifications); the backend already
        // persisted this notification in sendToCustomer. Adding a local copy here would duplicate it
        // (local UUID vs server _id never merge), so we only surface the system notification.
        showSystemNotification(title, body, type, orderId, deepLink);
    }

    @Override
    public void onNewToken(String token) {
        DeviceTokenManager.register(getApplicationContext(), token);
    }

    private void showSystemNotification(String title, String body, String type,
                                        String orderId, String deepLink) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (orderId != null) intent.putExtra(EXTRA_ORDER_ID, orderId);
        if (deepLink != null) intent.putExtra(EXTRA_DEEP_LINK, deepLink);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        int notificationId = (int) System.currentTimeMillis();
        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationId, intent, piFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, NotificationChannels.channelForType(type))
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
                || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            nm.notify(notificationId, builder.build());
        }
    }
}
