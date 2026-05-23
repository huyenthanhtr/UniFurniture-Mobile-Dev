package com.unifurniture.mobile.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.remote.ApiService;
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
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                result.setValue(null);
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
}
