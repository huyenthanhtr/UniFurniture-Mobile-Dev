package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Individual item sent within a CheckoutRequest.items[] array.
 * Mirrors what server's createCheckoutOrder expects per item.
 */
public class CheckoutItem {
    @SerializedName("variant_id")
    public String variantId;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("product_name")
    public String productName;
    @SerializedName("variant_name")
    public String variantName;
    public String sku;
    public int quantity;
    @SerializedName("unit_price")
    public double unitPrice;
}
