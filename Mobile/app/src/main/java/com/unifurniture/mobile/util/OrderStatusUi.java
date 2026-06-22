package com.unifurniture.mobile.util;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.unifurniture.mobile.R;

public final class OrderStatusUi {

    private OrderStatusUi() {
    }

    public static String normalize(String status) {
        String value = String.valueOf(status == null ? "" : status).trim().toLowerCase();
        if ("cancel_requested".equals(value)) {
            return "cancel_pending";
        }
        return value;
    }

    public static int labelRes(String status) {
        switch (normalize(status)) {
            case "pending":
                return R.string.status_pending;
            case "confirmed":
                return R.string.status_confirmed;
            case "processing":
                return R.string.status_processing;
            case "shipping":
                return R.string.status_shipping;
            case "delivered":
                return R.string.status_delivered;
            case "completed":
                return R.string.status_completed;
            case "cancel_pending":
                return R.string.status_cancel_pending;
            case "cancelled":
                return R.string.status_cancelled;
            default:
                return 0;
        }
    }

    public static String label(Context context, String status) {
        int resId = labelRes(status);
        return resId != 0 ? context.getString(resId) : String.valueOf(status == null ? "-" : status);
    }

    public static int textColorRes(String status) {
        switch (normalize(status)) {
            case "pending":
                return R.color.status_pending_text;
            case "confirmed":
                return R.color.status_confirmed_text;
            case "processing":
                return R.color.status_processing_text;
            case "shipping":
                return R.color.status_shipping_text;
            case "delivered":
                return R.color.status_delivered_text;
            case "completed":
                return R.color.status_completed_text;
            case "cancel_pending":
                return R.color.status_cancel_pending_text;
            case "cancelled":
                return R.color.status_cancelled_text;
            default:
                return R.color.gray_700;
        }
    }

    public static int backgroundColorRes(String status) {
        switch (normalize(status)) {
            case "pending":
                return R.color.status_pending_bg;
            case "confirmed":
                return R.color.status_confirmed_bg;
            case "processing":
                return R.color.status_processing_bg;
            case "shipping":
                return R.color.status_shipping_bg;
            case "delivered":
                return R.color.status_delivered_bg;
            case "completed":
                return R.color.status_completed_bg;
            case "cancel_pending":
                return R.color.status_cancel_pending_bg;
            case "cancelled":
                return R.color.status_cancelled_bg;
            default:
                return R.color.gray_100;
        }
    }

    public static int strokeColorRes(String status) {
        switch (normalize(status)) {
            case "pending":
                return R.color.status_pending_stroke;
            case "confirmed":
                return R.color.status_confirmed_stroke;
            case "processing":
                return R.color.status_processing_stroke;
            case "shipping":
                return R.color.status_shipping_stroke;
            case "delivered":
                return R.color.status_delivered_stroke;
            case "completed":
                return R.color.status_completed_stroke;
            case "cancel_pending":
                return R.color.status_cancel_pending_stroke;
            case "cancelled":
                return R.color.status_cancelled_stroke;
            default:
                return R.color.gray_300;
        }
    }

    public static void applyBadge(TextView textView, String status) {
        Context context = textView.getContext();
        textView.setText(label(context, status));
        textView.setTextColor(ContextCompat.getColor(context, textColorRes(status)));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(context, 999));
        background.setColor(ContextCompat.getColor(context, backgroundColorRes(status)));
        background.setStroke((int) dp(context, 1), ContextCompat.getColor(context, strokeColorRes(status)));
        textView.setBackground(background);
    }

    private static float dp(Context context, int value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
