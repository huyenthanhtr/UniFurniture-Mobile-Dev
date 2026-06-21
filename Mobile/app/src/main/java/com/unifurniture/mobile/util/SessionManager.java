package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.unifurniture.mobile.data.model.CustomerDto;
import com.google.gson.Gson;

public class SessionManager {

    private static final String PREF_NAME = "unifurniture_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_CUSTOMER = "customer";
    private static final String KEY_CUSTOMER_ID = "customer_id_raw";
    private static final String KEY_CART_ID = "cart_id";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_LOCAL_WISHLIST = "local_wishlist";
    private static final String KEY_AVATAR_URL = "avatar_url";

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

    // Token
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // Customer
    public void saveCustomer(CustomerDto customer) {
        if (customer != null) {
            prefs.edit().putString(KEY_CUSTOMER_ID, customer.getId()).apply();
        }
        prefs.edit().putString(KEY_CUSTOMER, gson.toJson(customer)).apply();
    }

    public void saveCustomerId(String customerId) {
        prefs.edit().putString(KEY_CUSTOMER_ID, customerId).apply();
    }

    public CustomerDto getCustomer() {
        String json = prefs.getString(KEY_CUSTOMER, null);
        if (json == null) return null;
        return gson.fromJson(json, CustomerDto.class);
    }

    public String getCustomerId() {
        String rawId = prefs.getString(KEY_CUSTOMER_ID, null);
        if (rawId != null) return rawId;
        CustomerDto c = getCustomer();
        return c != null ? c.getId() : null;
    }

    // Avatar URL cache
    public void saveAvatarUrl(String url) {
        prefs.edit().putString(KEY_AVATAR_URL, url).apply();
    }

    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, null);
    }

    // Profile ID (for change password)
    public void saveProfileId(String profileId) {
        prefs.edit().putString(KEY_PROFILE_ID, profileId).apply();
    }

    public String getProfileId() {
        return prefs.getString(KEY_PROFILE_ID, null);
    }

    // Cart ID cache
    public void saveCartId(String cartId) {
        prefs.edit().putString(KEY_CART_ID, cartId).apply();
    }

    public String getCartId() {
        return prefs.getString(KEY_CART_ID, null);
    }

    // Local Wishlist (for guest users)
    public void addToLocalWishlist(String productId) {
        java.util.Set<String> wishlist = getLocalWishlist();
        wishlist.add(productId);
        prefs.edit().putStringSet(KEY_LOCAL_WISHLIST, wishlist).apply();
    }

    public void removeFromLocalWishlist(String productId) {
        java.util.Set<String> wishlist = getLocalWishlist();
        wishlist.remove(productId);
        prefs.edit().putStringSet(KEY_LOCAL_WISHLIST, wishlist).apply();
    }

    public boolean isInLocalWishlist(String productId) {
        return getLocalWishlist().contains(productId);
    }

    public java.util.Set<String> getLocalWishlist() {
        return new java.util.HashSet<>(prefs.getStringSet(KEY_LOCAL_WISHLIST, new java.util.HashSet<>()));
    }

    public void clearLocalWishlist() {
        prefs.edit().remove(KEY_LOCAL_WISHLIST).apply();
    }

    // Logout - clear all session data
    public void logout() {
        prefs.edit().clear().apply();
    }
}
