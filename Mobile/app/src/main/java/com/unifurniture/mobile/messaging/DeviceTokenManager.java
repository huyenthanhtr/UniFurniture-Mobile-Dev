package com.unifurniture.mobile.messaging;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.unifurniture.mobile.data.model.DeviceTokenRequest;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Registers/unregisters the current FCM device token with the backend for the logged-in customer. */
public final class DeviceTokenManager {

    private static final String TAG = "DeviceTokenManager";

    private DeviceTokenManager() {}

    /** Fetch the current FCM token and register it for the logged-in customer. No-op if not logged in. */
    public static void syncToken(Context ctx) {
        if (!SessionManager.getInstance(ctx).isLoggedIn()) return;
        Context app = ctx.getApplicationContext();
        
        // Always subscribe to general broadcast topics for logged-in users
        FirebaseMessaging.getInstance().subscribeToTopic("promotions")
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Subscribed to promotions topic"))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to subscribe to promotions", e));
                
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> register(app, token))
                .addOnFailureListener(e -> Log.w(TAG, "getToken failed", e));
    }

    /** Register a known token for the currently logged-in customer. */
    public static void register(Context ctx, String token) {
        String customerId = SessionManager.getInstance(ctx).getCustomerId();
        if (customerId == null || token == null || token.isEmpty()) return;
        Log.d("FCM_TOKEN", "Current FCM Token: " + token);
        ApiClient.getInstance()
                .registerDeviceToken(new DeviceTokenRequest(customerId, token))
                .enqueue(noop("register"));
    }

    /**
     * Unregister this device's token for the given customer. Pass the customerId explicitly because
     * the session is usually cleared (logout) right after this call.
     */
    public static void unregister(Context ctx, String customerId) {
        if (customerId == null) return;
        Context app = ctx.getApplicationContext();
        
        // Unsubscribe from general broadcast topics upon logout
        FirebaseMessaging.getInstance().unsubscribeFromTopic("promotions")
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Unsubscribed from promotions topic"));
                
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) return;
                    ApiClient.getInstance()
                            .unregisterDeviceToken(new DeviceTokenRequest(customerId, token))
                            .enqueue(noop("unregister"));
                })
                .addOnFailureListener(e -> Log.w(TAG, "getToken failed", e));
    }

    private static Callback<Void> noop(String op) {
        return new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, op + " token http " + response.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.w(TAG, op + " token failed", t);
            }
        };
    }
}
