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
            case "exchange_pending":
                return R.string.status_exchange_pending;
            case "cancelled":
                return R.string.status_cancelled;
            case "exchanged":
                return R.string.status_exchanged;
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
            case "exchange_pending":
                return R.color.status_processing_text;
            case "cancelled":
                return R.color.status_cancelled_text;
            case "exchanged":
                return R.color.status_completed_text;
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
            case "exchange_pending":
                return R.color.status_processing_bg;
            case "cancelled":
                return R.color.status_cancelled_bg;
            case "exchanged":
                return R.color.status_completed_bg;
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
            case "exchange_pending":
                return R.color.status_processing_stroke;
            case "cancelled":
                return R.color.status_cancelled_stroke;
            case "exchanged":
                return R.color.status_completed_stroke;
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

    public static void applyTimeline(
            String status,
            android.widget.ImageView iv1, android.widget.TextView tv1, android.view.View l1,
            android.widget.ImageView iv2, android.widget.TextView tv2, android.view.View l2,
            android.widget.ImageView iv3, android.widget.TextView tv3, android.view.View l3,
            android.widget.ImageView iv4, android.widget.TextView tv4
    ) {
        Context context = iv1.getContext();
        String s = normalize(status);

        int inactiveColor = ContextCompat.getColor(context, R.color.gray_400);
        int inactiveLine = ContextCompat.getColor(context, R.color.gray_300);
        int activeColor = ContextCompat.getColor(context, R.color.primary);
        int errorColor = ContextCompat.getColor(context, R.color.error);

        // Reset all
        iv1.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tv1.setTextColor(inactiveColor);
        if (l1 != null) l1.setBackgroundColor(inactiveLine);
        
        iv2.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tv2.setTextColor(inactiveColor);
        if (l2 != null) l2.setBackgroundColor(inactiveLine);
        
        iv3.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tv3.setTextColor(inactiveColor);
        if (l3 != null) l3.setBackgroundColor(inactiveLine);
        
        iv4.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tv4.setTextColor(inactiveColor);

        // Set labels
        tv1.setText(R.string.timeline_step_1);
        tv2.setText(R.string.timeline_step_2);
        tv3.setText(R.string.timeline_step_3);
        tv4.setText(R.string.timeline_step_4);

        if ("cancelled".equals(s)) {
            iv1.setImageTintList(android.content.res.ColorStateList.valueOf(errorColor));
            tv1.setTextColor(errorColor);
            tv1.setText(R.string.status_cancelled);
            return;
        }

        if ("cancel_pending".equals(s)) {
            iv1.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tv1.setTextColor(activeColor);
            
            iv2.setImageTintList(android.content.res.ColorStateList.valueOf(errorColor));
            tv2.setTextColor(errorColor);
            tv2.setText(R.string.status_cancel_pending);
            return;
        }

        if ("exchange_pending".equals(s)) {
            iv1.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tv1.setTextColor(activeColor);
            if (l1 != null) l1.setBackgroundColor(activeColor);
            iv2.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tv2.setTextColor(activeColor);
            if (l2 != null) l2.setBackgroundColor(activeColor);
            iv3.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tv3.setTextColor(activeColor);
            if (l3 != null) l3.setBackgroundColor(activeColor);
            iv4.setImageTintList(android.content.res.ColorStateList.valueOf(errorColor));
            tv4.setTextColor(errorColor);
            tv4.setText(R.string.status_exchange_pending);
            return;
        }

        // Step 1 active for all non-cancelled
        iv1.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
        tv1.setTextColor(activeColor);

        if ("pending".equals(s)) return;

        // Step 2 active
        if (l1 != null) l1.setBackgroundColor(activeColor);
        iv2.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
        tv2.setTextColor(activeColor);

        if ("confirmed".equals(s) || "processing".equals(s)) return;

        // Step 3 active
        if (l2 != null) l2.setBackgroundColor(activeColor);
        iv3.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
        tv3.setTextColor(activeColor);

        if ("shipping".equals(s)) return;

        // Step 4 active
        if (l3 != null) l3.setBackgroundColor(activeColor);
        iv4.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
        tv4.setTextColor(activeColor);
        
        if ("delivered".equals(s)) {
            tv4.setText(R.string.status_delivered);
        } else if ("completed".equals(s)) {
            tv4.setText(R.string.status_completed);
        } else if ("exchanged".equals(s)) {
            tv4.setText(R.string.status_exchanged);
        }
    }

    private static float dp(Context context, int value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
