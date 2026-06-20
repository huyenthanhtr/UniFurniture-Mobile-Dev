package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Securely stores the user's login credentials for the "Remember password" feature.
 *
 * Backed by {@link EncryptedSharedPreferences} (AES-256). All access is wrapped in
 * try/catch with a plaintext-free fallback: if the Android keystore is unavailable or a
 * key becomes invalidated on a given device, we simply behave as "nothing remembered"
 * instead of crashing — important for fresh installs on diverse physical devices.
 */
public class CredentialStore {

    private static final String TAG = "CredentialStore";
    private static final String FILE_NAME = "unifurniture_secure_creds";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";

    private static CredentialStore instance;
    private SharedPreferences prefs; // null if encryption could not be initialised

    private CredentialStore(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to init encrypted prefs; remember-password disabled", e);
            prefs = null;
        }
    }

    public static synchronized CredentialStore getInstance(Context context) {
        if (instance == null) {
            instance = new CredentialStore(context);
        }
        return instance;
    }

    public boolean isRemembered() {
        try {
            return prefs != null && prefs.getBoolean(KEY_REMEMBER, false);
        } catch (Exception e) {
            return false;
        }
    }

    public String getPhone() {
        try {
            return prefs != null ? prefs.getString(KEY_PHONE, "") : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getPassword() {
        try {
            return prefs != null ? prefs.getString(KEY_PASSWORD, "") : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Persist credentials encrypted so they can pre-fill the login form next time. */
    public void save(String phone, String password) {
        if (prefs == null) return;
        try {
            prefs.edit()
                    .putBoolean(KEY_REMEMBER, true)
                    .putString(KEY_PHONE, phone)
                    .putString(KEY_PASSWORD, password)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save credentials", e);
        }
    }

    /** Forget any stored credentials (called when the checkbox is unticked). */
    public void clear() {
        if (prefs == null) return;
        try {
            prefs.edit().clear().apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear credentials", e);
        }
    }
}
