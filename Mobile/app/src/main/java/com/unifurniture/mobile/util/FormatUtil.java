package com.unifurniture.mobile.util;

import android.content.Context;
import android.text.format.DateFormat;
import java.text.NumberFormat;
import java.util.Locale;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ProductVariantDto;

public class FormatUtil {

    /**
     * Localized variant label (Cách 3): resolves the color from the server {@code color_i18n} map
     * for the current language, falling back to the client-side {@link ColorUi} localizer. The spec
     * part is also run through {@link ColorUi} so any color word inside it is localized too. Use this
     * overload wherever a {@link Context} is available; the context-less one below is for non-display
     * use (e.g. search matching) and stays raw.
     */
    public static String getVariantLabel(Context ctx, ProductVariantDto variant) {
        if (variant == null) return ctx.getString(R.string.variant_default_name);

        String color = LocalizedText.pickColor(ctx, variant.colorI18n, variant.color);
        color = (color == null) ? "" : color.trim();

        String specRaw = "";
        if (notBlank(variant.variantName)) specRaw = variant.variantName.trim();
        else if (notBlank(variant.name)) specRaw = variant.name.trim();
        else if (notBlank(variant.label)) specRaw = variant.label.trim();
        if (specRaw.startsWith("Combo Bàn Ăn ")) specRaw = specRaw.substring("Combo Bàn Ăn ".length());
        // Prefer the server-provided localized spec (name_i18n, Cách 3); fall back to the ColorUi
        // word localizer on the raw string for legacy variants without name_i18n.
        String spec = specRaw.isEmpty() ? "" : LocalizedText.pick(ctx, variant.nameI18n, specRaw);

        if (!color.isEmpty() && !spec.isEmpty()) {
            if (color.equalsIgnoreCase(spec)) return color;
            if (spec.toLowerCase().contains(color.toLowerCase())) return spec;
            if (color.toLowerCase().contains(spec.toLowerCase())) return color;
            return color + " - " + spec;
        } else if (!color.isEmpty()) {
            return color;
        } else if (!spec.isEmpty()) {
            return spec;
        } else {
            return ctx.getString(R.string.variant_default_name);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty() && !s.equals("undefined");
    }

    public static String getVariantLabel(ProductVariantDto variant) {
        if (variant == null) return "Mặc định";
        
        String color = (variant.color != null) ? variant.color.trim() : "";
        
        String spec = "";
        if (variant.variantName != null && !variant.variantName.trim().isEmpty() && !variant.variantName.equals("undefined")) {
            spec = variant.variantName.trim();
        } else if (variant.name != null && !variant.name.trim().isEmpty() && !variant.name.equals("undefined")) {
            spec = variant.name.trim();
        } else if (variant.label != null && !variant.label.trim().isEmpty() && !variant.label.equals("undefined")) {
            spec = variant.label.trim();
        }

        if (spec.startsWith("Combo Bàn Ăn ")) {
            spec = spec.substring("Combo Bàn Ăn ".length());
        }

        if (!color.isEmpty() && !spec.isEmpty()) {
            if (color.equalsIgnoreCase(spec)) {
                return color;
            } else if (spec.toLowerCase().contains(color.toLowerCase())) {
                return spec;
            } else if (color.toLowerCase().contains(spec.toLowerCase())) {
                return color;
            }
            return color + " - " + spec;
        } else if (!color.isEmpty()) {
            return color;
        } else if (!spec.isEmpty()) {
            return spec;
        } else {
            return "Mặc định";
        }
    }

    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /**
     * Format currency: 1500000 → "1.500.000 ₫"
     */
    public static String formatCurrency(double amount) {
        return VND_FORMAT.format((long) amount) + " ₫";
    }

    /**
     * Format currency from Double (nullable)
     */
    public static String formatCurrency(Double amount) {
        if (amount == null) return "Liên hệ";
        return formatCurrency(amount.doubleValue());
    }

    /**
     * Short format for large numbers: 1500 → "1.5k"
     */
    public static String formatSold(int sold) {
        if (sold >= 1000) return String.format(Locale.US, "%.1fk", sold / 1000.0);
        return String.valueOf(sold);
    }

    /**
     * Discount badge text
     */
    public static String discountBadge(Double price, Double originalPrice) {
        if (price == null || originalPrice == null || originalPrice <= price) return null;
        int pct = (int) Math.round((originalPrice - price) / originalPrice * 100);
        return "-" + pct + "%";
    }

    public static int dpToPx(android.content.Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static String stripDiacritics(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        String result = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return result.replace("đ", "d").replace("Đ", "D");
    }


    private FormatUtil() {}
}
