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

    /**
     * GET /cart/active?customer_id=...
     * Server returns: { cart: {...}, items: [...] }
     */
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

    /**
     * POST /cart/items/upsert
     * Server expects: { cart_id, variant_id, quantity, unit_price }
     * Server returns: single populated CartItemDto
     *
     * After upsert, we reload the full cart to update the UI.
     */
    public LiveData<CartItemDto> upsertCartItem(String cartId, String variantId,
                                                 int quantity, double unitPrice) {
        MutableLiveData<CartItemDto> result = new MutableLiveData<>();
        Map<String, Object> body = new HashMap<>();
        body.put("cart_id", cartId);
        body.put("variant_id", variantId);
        body.put("quantity", quantity);
        body.put("unit_price", unitPrice);

        apiService.upsertCartItem(body).enqueue(new Callback<CartItemDto>() {
            @Override
            public void onResponse(Call<CartItemDto> call, Response<CartItemDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<CartItemDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    /**
     * PATCH /cart/items/:id
     * Server expects: { quantity }
     * Server returns: { merged, item }  — we just signal success
     */
    public LiveData<Boolean> updateCartItemQuantity(String cartItemId, int quantity) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        Map<String, Integer> body = new HashMap<>();
        body.put("quantity", quantity);

        apiService.updateCartItem(cartItemId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                result.setValue(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    /**
     * DELETE /cart/items/:id
     * Server returns: { success, deleted }
     */
    public LiveData<Boolean> removeCartItem(String cartItemId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        apiService.deleteCartItem(cartItemId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                result.setValue(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }
}
