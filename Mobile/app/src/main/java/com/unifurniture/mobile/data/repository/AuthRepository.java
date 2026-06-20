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

    /** Build an AuthResponse from any HTTP response, carrying the server's message + success flag. */
    private static AuthResponse toResult(Response<AuthResponse> response) {
        AuthResponse ar = response.isSuccessful() ? response.body() : parseError(response);
        if (ar == null) ar = new AuthResponse();
        ar.success = response.isSuccessful();
        return ar;
    }

    private static AuthResponse parseError(Response<AuthResponse> response) {
        try {
            if (response.errorBody() != null) {
                AuthResponse ar = new com.google.gson.Gson()
                        .fromJson(response.errorBody().charStream(), AuthResponse.class);
                if (ar != null) return ar;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static AuthResponse networkError() {
        AuthResponse ar = new AuthResponse();
        ar.success = false;
        return ar;
    }

    // Login accepts a phone OR email. The backend reads the `emailOrPhone` key; `phone` is sent too
    // for compatibility with older server builds.
    public LiveData<AuthResponse> login(String emailOrPhone, String password) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("emailOrPhone", emailOrPhone);
        body.put("phone", emailOrPhone);
        body.put("password", password);
        apiService.login(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(toResult(response));
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(networkError());
            }
        });
        return result;
    }

    // Register matches the web payload: full_name, phone (already formatted), password_hash (raw
    // password — the server hashes it), and optional email.
    public LiveData<AuthResponse> register(String phone, String password, String fullName, String email) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("full_name", fullName);
        body.put("phone", phone);
        body.put("password_hash", password);
        if (email != null && !email.trim().isEmpty()) {
            body.put("email", email.trim());
        }
        apiService.register(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(toResult(response));
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(networkError());
            }
        });
        return result;
    }

    public LiveData<AuthResponse> verifyOtp(String phone, String otp) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("otp", otp);
        apiService.verifyOtp(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(toResult(response));
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(networkError());
            }
        });
        return result;
    }

    public LiveData<AuthResponse> forgotPassword(String phone) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
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

    public LiveData<AuthResponse> resetPassword(String phone, String otp, String newPassword) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("otp", otp);
        body.put("password", newPassword);
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
