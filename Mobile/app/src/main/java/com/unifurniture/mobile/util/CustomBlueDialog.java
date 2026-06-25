package com.unifurniture.mobile.util;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.unifurniture.mobile.R;

public class CustomBlueDialog {

    public interface OnClickListener {
        void onClick(AlertDialog dialog);
    }

    /**
     * Convenience for a single-button error popup, styled like the rest of the app's blue dialogs.
     * Used for auth (login/register/OTP/forgot) errors so they stand out instead of a faint Toast.
     */
    public static void showError(Context context, String message) {
        show(context,
                R.drawable.ic_alert,
                context.getString(R.string.error_title),
                message,
                context.getString(R.string.ok),
                AlertDialog::dismiss,
                null,
                null);
    }

    /** "No internet" popup with a Retry action and a dismiss button. */
    public static void showNoInternet(Context context, Runnable onRetry) {
        show(context,
                R.drawable.ic_alert,
                context.getString(R.string.no_internet_title),
                context.getString(R.string.no_internet_message),
                context.getString(R.string.retry),
                dialog -> {
                    dialog.dismiss();
                    if (onRetry != null) onRetry.run();
                },
                context.getString(R.string.close),
                AlertDialog::dismiss);
    }

    public static void show(
            Context context,
            int iconResId,
            String title,
            String message,
            String positiveText,
            OnClickListener positiveListener,
            String negativeText,
            OnClickListener negativeListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_blue, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivIcon = view.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        MaterialButton btnPositive = view.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = view.findViewById(R.id.btnNegative);

        if (iconResId != 0) {
            ivIcon.setImageResource(iconResId);
        }

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (positiveText != null) {
            btnPositive.setText(positiveText);
            btnPositive.setVisibility(View.VISIBLE);
            btnPositive.setOnClickListener(v -> {
                if (positiveListener != null) {
                    positiveListener.onClick(dialog);
                } else {
                    dialog.dismiss();
                }
            });
        } else {
            btnPositive.setVisibility(View.GONE);
        }

        if (negativeText != null) {
            btnNegative.setText(negativeText);
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setOnClickListener(v -> {
                if (negativeListener != null) {
                    negativeListener.onClick(dialog);
                } else {
                    dialog.dismiss();
                }
            });
        } else {
            btnNegative.setVisibility(View.GONE);
        }

        dialog.setCancelable(false);
        dialog.show();
    }
}
