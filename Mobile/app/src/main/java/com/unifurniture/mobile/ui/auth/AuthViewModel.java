package com.unifurniture.mobile.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.AuthResponse;
import com.unifurniture.mobile.data.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<AuthResponse> authResult = new MutableLiveData<>();
    private final MutableLiveData<String> registerSuccess = new MutableLiveData<>(); // formatted phone
    private final MutableLiveData<Boolean> otpSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(UniFurnitureApp.getInstance().getApiService());
    }

    private String str(int id) {
        return getApplication().getString(id);
    }

    private String msgOr(AuthResponse r, int fallback) {
        if (r != null && r.message != null && !r.message.isEmpty()) return r.message;
        return str(fallback);
    }

    /** Format a Vietnamese phone for the API like the web: strip a leading 0, prepend country code 84. */
    public static String formatPhoneForApi(String raw) {
        String p = raw == null ? "" : raw.trim().replaceAll("\\s+", "");
        if (p.startsWith("0")) p = p.substring(1);
        return "84" + p;
    }

    public void login(String emailOrPhone, String password) {
        if (emailOrPhone.isEmpty() || password.isEmpty()) {
            error.setValue(str(R.string.error_fill_info));
            return;
        }
        loading.setValue(true);
        repository.login(emailOrPhone, password).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.success && response.token != null) {
                authResult.setValue(response);
            } else {
                error.setValue(msgOr(response, R.string.error_login_failed));
            }
        });
    }

    public void register(String fullName, String phone, String email, String password, String confirmPassword) {
        if (fullName.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            error.setValue(str(R.string.error_fill_info));
            return;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 9 || digits.length() > 10) {
            error.setValue(str(R.string.error_phone_invalid));
            return;
        }
        if (password.length() < 8) {
            error.setValue(str(R.string.error_password_short));
            return;
        }
        if (!password.equals(confirmPassword)) {
            error.setValue(str(R.string.error_password_mismatch));
            return;
        }
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error.setValue(str(R.string.error_email_invalid));
            return;
        }
        String formatted = formatPhoneForApi(phone);
        loading.setValue(true);
        repository.register(formatted, password, fullName, email).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.success) {
                registerSuccess.setValue(formatted);
            } else {
                error.setValue(msgOr(response, R.string.error_register_failed));
            }
        });
    }

    public void verifyOtp(String phone, String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            error.setValue(str(R.string.error_fill_info));
            return;
        }
        loading.setValue(true);
        repository.verifyOtp(phone, otp).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.success) {
                otpSuccess.setValue(true);
            } else {
                error.setValue(msgOr(response, R.string.error_invalid_otp_forgot));
            }
        });
    }

    public void forgotPassword(String phone) {
        loading.setValue(true);
        repository.forgotPassword(phone).observeForever(response -> {
            loading.setValue(false);
            if (response != null) {
                authResult.setValue(response);
            } else {
                error.setValue(str(R.string.error_phone_not_found));
            }
        });
    }

    public void resetPassword(String phone, String otp, String newPassword) {
        loading.setValue(true);
        repository.resetPassword(phone, otp, newPassword).observeForever(response -> {
            loading.setValue(false);
            if (response != null) {
                authResult.setValue(response);
            } else {
                error.setValue(str(R.string.error_invalid_otp_forgot));
            }
        });
    }

    // One-shot consumers clear these after handling so they don't re-fire on fragment re-creation.
    public void clearRegisterSuccess() { registerSuccess.setValue(null); }
    public void clearOtpSuccess() { otpSuccess.setValue(null); }

    public LiveData<AuthResponse> getAuthResult() { return authResult; }
    public LiveData<String> getRegisterSuccess() { return registerSuccess; }
    public LiveData<Boolean> getOtpSuccess() { return otpSuccess; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
