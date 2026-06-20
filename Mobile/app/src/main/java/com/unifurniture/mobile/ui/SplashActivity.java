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

        new Handler().postDelayed(() -> {
            // Auto navigate to Main or Auth based on session
            Intent intent;
            if (SessionManager.getInstance(this).isLoggedIn()) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class); // Guest mode OK
            }
            startActivity(intent);
            finish();
        }, 1800);
    }
}
