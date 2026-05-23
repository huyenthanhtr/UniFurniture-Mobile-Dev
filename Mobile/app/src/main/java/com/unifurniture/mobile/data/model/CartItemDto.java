package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CartItemDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("cart_id")
    public String cartId;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("variant_id")
    public String variantId;
    public Integer quantity;
    public Double price;
    public ProductDto product;
    public ProductVariantDto variant;

    public double getTotalPrice() {
        double unitPrice = price != null ? price : 0;
        int qty = quantity != null ? quantity : 1;
        return unitPrice * qty;
    }
}
