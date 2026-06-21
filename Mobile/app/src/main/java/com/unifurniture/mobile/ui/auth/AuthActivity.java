package com.unifurniture.mobile.ui.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.unifurniture.mobile.R;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new LoginFragment())
                    .commit();
        }
    }
}
