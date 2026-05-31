package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Server GET /cart/active response: { cart: {...}, items: [...] }
 */
public class CartDto {
    public CartInfo cart;
    public List<CartItemDto> items;

    public static class CartInfo {
        @SerializedName("_id")
        public String id;
        @SerializedName("customer_id")
        public String customerId;
        public String status;
    }

    /**
     * Helper: get cart id from nested cart info
     */
    public String getCartId() {
        return cart != null ? cart.id : null;
    }
}
