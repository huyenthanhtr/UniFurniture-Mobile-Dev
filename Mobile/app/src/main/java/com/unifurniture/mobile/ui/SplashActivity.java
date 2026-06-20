package com.unifurniture.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.unifurniture.mobile.util.LanguageHelper.updateBaseContextLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Sec 1: on first launch (no remembered choice) show the language picker
        // immediately; otherwise continue to the app after a short splash.
        if (!com.unifurniture.mobile.util.LanguageHelper.isRemembered(this)) {
            com.unifurniture.mobile.util.LanguageDialog.show(this, true, (code, changed) -> goNext());
        } else {
            new Handler().postDelayed(this::goNext, 1800);
        }
    }

    private void goNext() {
        // Guest mode is allowed, so we always land on MainActivity. MainActivity's
        // attachBaseContext re-applies the chosen locale.
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
