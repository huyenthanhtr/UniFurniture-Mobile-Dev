package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class OrderDetailDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("order_id")
    public String orderId;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("variant_id")
    public String variantId;
    public Integer quantity;
    @SerializedName("unit_price")
    public Double unitPrice;
    public ProductDto product;
}
