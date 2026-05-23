package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.unifurniture.mobile.data.model.CustomerDto;
import com.google.gson.Gson;

public class SessionManager {

    private static final String PREF_NAME = "unifurniture_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_CUSTOMER = "customer";
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

    // Token
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null && getCustomer() != null;
    }

    // Customer
    public void saveCustomer(CustomerDto customer) {
        prefs.edit().putString(KEY_CUSTOMER, gson.toJson(customer)).apply();
    }

    public CustomerDto getCustomer() {
        String json = prefs.getString(KEY_CUSTOMER, null);
        if (json == null) return null;
        return gson.fromJson(json, CustomerDto.class);
    }

    public String getCustomerId() {
        CustomerDto c = getCustomer();
        return c != null ? c.id : null;
    }

    // Cart ID cache
    public void saveCartId(String cartId) {
        prefs.edit().putString(KEY_CART_ID, cartId).apply();
    }

    public String getCartId() {
        return prefs.getString(KEY_CART_ID, null);
    }

    // Logout - clear all session data
    public void logout() {
        prefs.edit().clear().apply();
    }
}
