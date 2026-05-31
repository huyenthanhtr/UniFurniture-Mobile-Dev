package com.unifurniture.mobile.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.AuthResponse;
import com.unifurniture.mobile.data.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<AuthResponse> authResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    // Holds the phone used during register/forgot-password for OTP screen
    private final MutableLiveData<String> pendingPhone = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(UniFurnitureApp.getInstance().getApiService());
    }

    /**
     * Login — server expects { emailOrPhone, password } and returns { message, profile }.
     */
    public void login(String phone, String password) {
        if (phone.isEmpty() || password.isEmpty()) {
            error.setValue("Vui lòng nhập đủ thông tin");
            return;
        }
        loading.setValue(true);
        repository.login(phone, password).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.profile != null) {
                authResult.setValue(response);
            } else {
                error.setValue("Sai số điện thoại hoặc mật khẩu");
            }
        });
    }

    /**
     * Register — server returns { message } only. After success,
     * user must proceed to OTP screen. pendingPhone is stored for OTP.
     */
    public void register(String phone, String password, String name, String email) {
        if (phone.isEmpty() || password.isEmpty() || name.isEmpty()) {
            error.setValue("Vui lòng nhập đủ thông tin bắt buộc (*)");
            return;
        }
        loading.setValue(true);
        pendingPhone.setValue(phone);
        repository.register(phone, password, name, email).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.message != null) {
                // Signal UI to navigate to OTP screen (no profile yet)
                authResult.setValue(response);
            } else {
                error.setValue("Đăng ký thất bại. Vui lòng thử lại.");
            }
        });
    }

    /**
     * Verify OTP — server returns { message, profile }.
     * The UI should save profile to SessionManager on success.
     */
    public void verifyOtp(String phone, String otp) {
        loading.setValue(true);
        repository.verifyOtp(phone, otp).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.profile != null) {
                authResult.setValue(response);
            } else {
                error.setValue("Mã OTP không hợp lệ hoặc đã hết hạn");
            }
        });
    }

    /**
     * Forgot Password — server returns { message }. Stores phone for reset flow.
     */
    public void forgotPassword(String phone) {
        if (phone.isEmpty()) {
            error.setValue("Vui lòng nhập số điện thoại");
            return;
        }
        loading.setValue(true);
        pendingPhone.setValue(phone);
        repository.forgotPassword(phone).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.message != null) {
                authResult.setValue(response);
            } else {
                error.setValue("Số điện thoại chưa được đăng ký");
            }
        });
    }

    /**
     * Reset Password — server returns { message }.
     */
    public void resetPassword(String phone, String otp, String newPassword) {
        loading.setValue(true);
        repository.resetPassword(phone, otp, newPassword).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.message != null) {
                authResult.setValue(response);
            } else {
                error.setValue("Đặt lại mật khẩu thất bại");
            }
        });
    }

    public LiveData<AuthResponse> getAuthResult() { return authResult; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getPendingPhone() { return pendingPhone; }
}
