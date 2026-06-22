package com.unifurniture.mobile.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;

import com.unifurniture.mobile.R;

/**
 * Reusable language picker (English / Vietnamese / Chinese / French) with a
 * "Remember my choice" checkbox. Used for the first-launch popup (Sec 1) and the
 * in-app language switch from Profile (Sec 2).
 */
public class LanguageDialog {

    public interface OnLanguageSelected {
        /** @param changed true if the picked language differs from the one in effect. */
        void onSelected(String code, boolean changed);
    }

    private static String idToCode(int checkedId) {
        if (checkedId == R.id.rbVietnamese) return "vi";
        if (checkedId == R.id.rbChinese) return "zh";
        if (checkedId == R.id.rbFrench) return "fr";
        return "en";
    }

    private static int codeToId(String code) {
        if ("vi".equals(code)) return R.id.rbVietnamese;
        if ("zh".equals(code)) return R.id.rbChinese;
        if ("fr".equals(code)) return R.id.rbFrench;
        return R.id.rbEnglish;
    }

    /**
     * @param firstLaunch when true the dialog is non-cancelable and the remember box
     *                    defaults to checked (so the user isn't asked again).
     */
    public static void show(Context context, boolean firstLaunch, OnLanguageSelected callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_language, null, false);
        RadioGroup rg = view.findViewById(R.id.rgLanguages);
        CheckBox cbRemember = view.findViewById(R.id.cbRememberLanguage);

        final String previous = LanguageHelper.getLanguage(context);
        rg.check(codeToId(previous));
        cbRemember.setChecked(firstLaunch || LanguageHelper.isRemembered(context));

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(!firstLaunch)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String code = idToCode(rg.getCheckedRadioButtonId());
                    LanguageHelper.setLanguage(context, code);
                    LanguageHelper.setRemembered(context, cbRemember.isChecked());
                    if (callback != null) callback.onSelected(code, !code.equals(previous));
                });

        if (!firstLaunch) {
            builder.setNegativeButton(R.string.cancel, null);
        }
        builder.show();
    }
}
