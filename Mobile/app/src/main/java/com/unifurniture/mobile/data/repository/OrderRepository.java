package com.unifurniture.mobile.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.remote.ApiService;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderRepository {

    private final ApiService apiService;

    public OrderRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<ApiListResponse<OrderDto>> getOrders(String customerId) {
        MutableLiveData<ApiListResponse<OrderDto>> result = new MutableLiveData<>();
        apiService.getOrders(customerId).enqueue(new Callback<ApiListResponse<OrderDto>>() {
            @Override
            public void onResponse(Call<ApiListResponse<OrderDto>> call,
                                   Response<ApiListResponse<OrderDto>> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<ApiListResponse<OrderDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<OrderDto> getOrderById(String orderId) {
        MutableLiveData<OrderDto> result = new MutableLiveData<>();
        apiService.getOrderById(orderId).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<CheckoutResponse> createOrder(CheckoutRequest request) {
        MutableLiveData<CheckoutResponse> result = new MutableLiveData<>();
        apiService.createOrder(request).enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                if (response.isSuccessful()) {
                    result.setValue(response.body());
                } else {
                    CheckoutResponse error = new CheckoutResponse();
                    error.message = readErrorMessage(response);
                    result.setValue(error);
                }
            }
            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                CheckoutResponse error = new CheckoutResponse();
                error.message = t.getMessage();
                result.setValue(error);
            }
        });
        return result;
    }

    public LiveData<Boolean> requestCancelOrder(String orderId, String reason, String note, String phone) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("reason", reason);
        body.put("note", note);
        body.put("phone", phone);

        apiService.requestCancelOrder(orderId, body).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    public LiveData<Boolean> completeDemoTransfer(String orderId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        apiService.completeDemoTransfer(orderId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    public LiveData<ApiListResponse<OrderDto>> trackOrder(String trackingCode) {
        MutableLiveData<ApiListResponse<OrderDto>> result = new MutableLiveData<>();
        apiService.trackOrderByCode(trackingCode).enqueue(new Callback<ApiListResponse<OrderDto>>() {
            @Override
            public void onResponse(Call<ApiListResponse<OrderDto>> call,
                                   Response<ApiListResponse<OrderDto>> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<ApiListResponse<OrderDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    private String readErrorMessage(Response<?> response) {
        try {
            String raw = response.errorBody() != null ? response.errorBody().string() : "";
            if (raw == null || raw.trim().isEmpty()) {
                return "Error " + response.code();
            }

            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("error")) return json.get("error").getAsString();
            if (json.has("message")) return json.get("message").getAsString();
            return new Gson().fromJson(raw, Object.class).toString();
        } catch (Exception ignored) {
            return "Error " + response.code();
        }
    }
}
