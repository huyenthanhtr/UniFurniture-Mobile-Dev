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
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(UniFurnitureApp.getInstance().getApiService());
    }

    public void login(String phone, String password) {
        if (phone.isEmpty() || password.isEmpty()) {
            error.setValue(getApplication().getString(R.string.error_fill_info));
            return;
        }
        loading.setValue(true);
        repository.login(phone, password).observeForever(response -> {
            loading.setValue(false);
            if (response != null && response.token != null) {
                authResult.setValue(response);
            } else {
                error.setValue(getApplication().getString(R.string.error_login_failed));
            }
        });
    }

    public void register(String phone, String password, String name) {
        if (phone.isEmpty() || password.isEmpty() || name.isEmpty()) {
            error.setValue(getApplication().getString(R.string.error_fill_info));
            return;
        }
        loading.setValue(true);
        repository.register(phone, password, name).observeForever(response -> {
            loading.setValue(false);
            authResult.setValue(response);
        });
    }

    public void verifyOtp(String phone, String otp) {
        loading.setValue(true);
        repository.verifyOtp(phone, otp).observeForever(response -> {
            loading.setValue(false);
            authResult.setValue(response);
        });
    }

    public void forgotPassword(String phone) {
        loading.setValue(true);
        repository.forgotPassword(phone).observeForever(response -> {
            loading.setValue(false);
            if (response != null) {
                authResult.setValue(response);
            } else {
                error.setValue(getApplication().getString(R.string.error_phone_not_found));
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
                error.setValue(getApplication().getString(R.string.error_invalid_otp_forgot));
            }
        });
    }

    public LiveData<AuthResponse> getAuthResult() { return authResult; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
