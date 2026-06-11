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

    public LiveData<CartDto> getActiveCart(String customerId, String cartId) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        // Gửi cả 2 định danh nếu có để server xử lý logic merge giỏ hàng
        apiService.getActiveCart(customerId, cartId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                if (response.isSuccessful()) {
                    result.setValue(response.body());
                } else {
                    android.util.Log.e("CartRepo", "Get active cart failed with code: " + response.code());
                    result.setValue(null);
                }
            }
            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<CartDto> addToCart(String customerId, String cartId, String variantId, int quantity, MutableLiveData<String> errorMsg) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        
        android.util.Log.d("CartRepo", "addToCart Request Parameters:");
        android.util.Log.d("CartRepo", "  customer_id: " + (customerId != null ? customerId : "null"));
        android.util.Log.d("CartRepo", "  cart_id: " + (cartId != null ? cartId : "null"));
        android.util.Log.d("CartRepo", "  variant_id: " + (variantId != null ? variantId : "null"));
        android.util.Log.d("CartRepo", "  quantity: " + quantity);

        if (variantId == null || variantId.isEmpty()) {
            if (errorMsg != null) errorMsg.setValue("Vui lòng chọn loại sản phẩm");
            return result;
        }

        Map<String, Object> body = new HashMap<>();
        if (customerId != null && !customerId.isEmpty()) {
            body.put("customer_id", customerId);
        }
        if (cartId != null && !cartId.isEmpty()) {
            body.put("cart_id", cartId);
        }
        body.put("variant_id", variantId);
        body.put("quantity", Math.max(1, quantity));

        String jsonPayload = new com.google.gson.Gson().toJson(body);
        android.util.Log.d("CartRepo", "Final Add to Cart Payload: " + jsonPayload);

        apiService.upsertCartItem(body).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("CartRepo", "Add to cart success: " + new com.google.gson.Gson().toJson(response.body()));
                    result.setValue(response.body());
                } else {
                    String msg = "Error " + response.code();
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : null;
                        android.util.Log.e("CartRepo", "Add to cart Error Body: " + errorBody);
                        if (errorBody != null && !errorBody.isEmpty()) {
                            java.util.Map map = new com.google.gson.Gson().fromJson(errorBody, java.util.Map.class);
                            if (map != null && map.get("message") != null) {
                                msg = map.get("message").toString();
                            } else {
                                msg = errorBody;
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CartRepo", "Error parsing error body", e);
                    }
                    android.util.Log.e("CartRepo", "Add to cart failed message: " + msg);
                    if (errorMsg != null) errorMsg.setValue(msg);
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                android.util.Log.e("CartRepo", "Add to cart network failure", t);
                // We'll handle localized network error in the ViewModel/Activity or pass a generic one
                if (errorMsg != null) errorMsg.setValue("Network error: " + t.getMessage());
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<CartDto> updateCartItemQuantity(String cartItemId, int quantity) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        Map<String, Object> body = new HashMap<>();
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

    public LiveData<CartDto> updateCartItemVariant(String cartItemId, String variantId) {
        MutableLiveData<CartDto> result = new MutableLiveData<>();
        Map<String, Object> body = new HashMap<>();
        body.put("variant_id", variantId);

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
