package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LanguageHelper {

    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String KEY_LANGUAGE = "AppLanguage";
    private static final String KEY_REMEMBER = "RememberLanguage";

    /** Default app language is English (Sec 1). */
    public static final String DEFAULT_LANGUAGE = "en";

    /** Supported language codes, in display order (English first / default). */
    public static final String[] SUPPORTED_CODES = {"en", "vi", "zh", "fr"};

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setLanguage(Context context, String languageCode) {
        prefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    /** Whether the user ticked "Remember my choice" — if true we skip the first-launch popup. */
    public static void setRemembered(Context context, boolean remembered) {
        prefs(context).edit().putBoolean(KEY_REMEMBER, remembered).apply();
    }

    public static boolean isRemembered(Context context) {
        return prefs(context).getBoolean(KEY_REMEMBER, false);
    }

    public static Context updateBaseContextLocale(Context context) {
        String language = getLanguage(context);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
