package com.unifurniture.mobile.util;

import android.text.format.DateFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil {

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

    private FormatUtil() {}
}
