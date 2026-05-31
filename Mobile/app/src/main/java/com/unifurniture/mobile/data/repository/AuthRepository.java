package com.unifurniture.mobile.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.remote.ApiService;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService apiService;

    public AuthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Server expects: { emailOrPhone, password }
     * Server returns: { message, profile }
     */
    public LiveData<AuthResponse> login(String phone, String password) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("emailOrPhone", phone);
        body.put("password", password);
        apiService.login(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    // Helper to format phone for server (adds 84 country code)
    private String formatPhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.trim().replaceAll("\\D", "");
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }
        if (!cleaned.startsWith("84")) {
            cleaned = "84" + cleaned;
        }
        return cleaned;
    }

    /**
     * Server expects: { phone, password_hash, full_name, email?, gender?, date_of_birth?, address? }
     * Server returns: { message } — must proceed to OTP verification
     */
    public LiveData<AuthResponse> register(String phone, String password, String name, String email) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", formatPhone(phone));
        body.put("password_hash", password);
        body.put("full_name", name);
        if (email != null && !email.trim().isEmpty()) {
            body.put("email", email.trim());
        }
        apiService.register(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * Server expects: { phone, otp }
     * Server returns: { message, profile }
     */
    public LiveData<AuthResponse> verifyOtp(String phone, String otp) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", formatPhone(phone));
        body.put("otp", otp);
        apiService.verifyOtp(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * Server expects: { phone }
     * Server returns: { message }
     */
    public LiveData<AuthResponse> forgotPassword(String phone) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", formatPhone(phone));
        apiService.forgotPassword(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * Server expects: { phone, otp, newPassword }
     * Server returns: { message }
     */
    public LiveData<AuthResponse> resetPassword(String phone, String otp, String newPassword) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", formatPhone(phone));
        body.put("otp", otp);
        body.put("newPassword", newPassword);
        apiService.resetPassword(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }
}
