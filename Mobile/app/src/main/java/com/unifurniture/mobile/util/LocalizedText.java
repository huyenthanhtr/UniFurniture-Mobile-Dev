package com.unifurniture.mobile.util;

import android.content.Context;

import java.util.Map;

/**
 * Resolves a localized string for the current app language from a server-provided i18n map
 * (e.g. {@code {"vi":..,"en":..,"fr":..,"zh":..}}), the source of truth for translated product
 * data (Cách 3). When the map is absent or missing the current locale, it falls back to the
 * client-side {@link ColorUi} localizer on the raw value, then to the Vietnamese/default entry,
 * then to the raw value itself.
 */
public final class LocalizedText {

    private LocalizedText() {}

    public static String pickColor(Context ctx, Map<String, String> i18n, String rawFallback) {
        if (i18n != null) {
            String value = i18n.get(LanguageHelper.getLanguage(ctx));
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        String viaColorUi = ColorUi.label(ctx, rawFallback);
        if (!viaColorUi.isEmpty()) {
            return viaColorUi;
        }
        if (i18n != null) {
            String vi = i18n.get("vi");
            if (vi != null && !vi.trim().isEmpty()) {
                return vi.trim();
            }
        }
        return rawFallback == null ? "" : rawFallback.trim();
    }
}
