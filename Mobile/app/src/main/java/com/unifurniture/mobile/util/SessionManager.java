package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.unifurniture.mobile.data.model.ProfileDto;
import com.google.gson.Gson;

/**
 * Session manager — stores the logged-in Profile (not token + Customer).
 *
 * Server does NOT issue JWTs. Authentication is verified on the server by
 * matching the hashed password. On the client we simply persist the
 * ProfileDto returned by /auth/login or /auth/verify-otp.
 */
public class SessionManager {

    private static final String PREF_NAME = "unifurniture_session";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_CART_ID = "cart_id";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // ── Profile ─────────────────────────────────────────────────────────────
    public void saveProfile(ProfileDto profile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply();
    }

    public ProfileDto getProfile() {
        String json = prefs.getString(KEY_PROFILE, null);
        if (json == null) return null;
        return gson.fromJson(json, ProfileDto.class);
    }

    /**
     * profile._id — used as account_id throughout the API
     * (orders, wishlist, loyalty, cart customer_id).
     */
    public String getProfileId() {
        ProfileDto p = getProfile();
        return p != null ? p.id : null;
    }

    /**
     * Alias: many existing call-sites use getCustomerId().
     * After migration the value equals profile._id (which the server
     * uses as customer_id / account_id interchangeably).
     */
    public String getCustomerId() {
        return getProfileId();
    }

    public boolean isLoggedIn() {
        return getProfile() != null;
    }

    // ── Cart ID cache ───────────────────────────────────────────────────────
    public void saveCartId(String cartId) {
        prefs.edit().putString(KEY_CART_ID, cartId).apply();
    }

    public String getCartId() {
        return prefs.getString(KEY_CART_ID, null);
    }

    // ── Logout ──────────────────────────────────────────────────────────────
    public void logout() {
        prefs.edit().clear().apply();
    }
}
