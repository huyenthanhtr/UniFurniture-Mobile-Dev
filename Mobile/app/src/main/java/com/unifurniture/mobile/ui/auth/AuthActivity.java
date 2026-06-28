package com.unifurniture.mobile.ui.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.unifurniture.mobile.R;

public class AuthActivity extends AppCompatActivity {

    /**
     * AuthActivity is always launched for a result by MainActivity. Instead of touching the task
     * stack itself, it reports one of these outcomes when it finishes; MainActivity (still alive)
     * reacts, so its ViewModels, caches and FragmentManager are preserved (no recreate).
     */
    public static final int RESULT_GO_HOME = RESULT_FIRST_USER;
    public static final int RESULT_LOGGED_IN = RESULT_FIRST_USER + 1;

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

    /**
     * Sec 6: the user asked to return to Home (as a guest) from any auth screen. We don't touch
     * the task stack — we report the outcome to MainActivity (the launcher caller), which is still
     * alive and simply navigates to Home in place.
     */
    public void goHome() {
        setResult(RESULT_GO_HOME);
        finish();
    }

    /** Login succeeded: tell MainActivity to refresh onto Home with the new session, in place. */
    public void finishLoggedIn() {
        setResult(RESULT_LOGGED_IN);
        finish();
    }
}
