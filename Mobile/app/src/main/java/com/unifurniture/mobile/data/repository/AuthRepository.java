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

    public LiveData<AuthResponse> login(String phone, String password) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
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

    public LiveData<AuthResponse> register(String phone, String password, String name) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("password", password);
        body.put("name", name);
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

    public LiveData<AuthResponse> verifyOtp(String phone, String otp) {
        MutableLiveData<AuthResponse> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
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
