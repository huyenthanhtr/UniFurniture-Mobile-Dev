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

public class CartRepository {

    private final ApiService apiService;

    public CartRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<CartDto> getActiveCart(String customerId) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        apiService.getActiveCart(customerId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<CartDto> addToCart(String customerId, String productId,
                                       String variantId, int quantity) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        Map<String, Object> body = new HashMap<>();
        body.put("customer_id", customerId);
        body.put("product_id", productId);
        if (variantId != null) body.put("variant_id", variantId);
        body.put("quantity", quantity);

        apiService.upsertCartItem(body).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<CartDto> updateCartItemQuantity(String cartItemId, int quantity) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        Map<String, Integer> body = new HashMap<>();
        body.put("quantity", quantity);

        apiService.updateCartItem(cartItemId, body).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<CartDto> removeCartItem(String cartItemId) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        apiService.deleteCartItem(cartItemId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }
}
