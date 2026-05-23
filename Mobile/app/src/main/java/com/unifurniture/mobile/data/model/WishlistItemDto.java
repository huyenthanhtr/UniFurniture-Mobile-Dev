package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class WishlistItemDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("customer_id")
    public String customerId;
    @SerializedName("product_id")
    public String productId;
    public ProductDto product;
}
