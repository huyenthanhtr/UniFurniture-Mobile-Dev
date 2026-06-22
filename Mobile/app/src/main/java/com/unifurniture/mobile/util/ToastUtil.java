package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.unifurniture.mobile.R;

public class ToastUtil {

    public static void show(Context context, String message) {
        showCustomToast(context, message, false);
    }

    public static void show(Context context, int resId) {
        showCustomToast(context, context.getString(resId), false);
    }

    public static void error(Context context, String message) {
        showCustomToast(context, message, true);
    }

    public static void error(Context context, int resId) {
        showCustomToast(context, context.getString(resId), true);
    }

    private static void showCustomToast(Context context, String message, boolean isError) {
        if (context == null) return;

        try {
            // Create container
            MaterialCardView cardView = new MaterialCardView(context);
            cardView.setCardElevation(10f);
            cardView.setRadius(50f); // Pill shape
            cardView.setStrokeWidth(0);
            
            // Set background color: Deep Blue for normal, Red for error
            int bgColor = isError ? Color.parseColor("#E53935") : Color.parseColor("#0288D1");
            cardView.setCardBackgroundColor(ColorStateList.valueOf(bgColor));

            // Content Layout
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(40, 24, 56, 24);

            // Icon
            ImageView iconView = new ImageView(context);
            iconView.setImageResource(isError ? android.R.drawable.ic_dialog_alert : R.drawable.ic_check);
            iconView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
            iconParams.setMargins(0, 0, 24, 0);
            iconView.setLayoutParams(iconParams);
            layout.addView(iconView);

            // Text
            TextView textView = new TextView(context);
            textView.setText(message);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(15);
            textView.setLineSpacing(0, 1.1f);
            layout.addView(textView);

            cardView.addView(layout);

            Toast toast = new Toast(context.getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(cardView);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 250);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
