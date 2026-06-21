package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProductImageDto {
    @SerializedName(value = "_id", alternate = {"id"})
    public String id;
    @SerializedName(value = "product_id", alternate = {"productId"})
    public String productId;
    @SerializedName(value = "variant_id", alternate = {"variantId"})
    public String variantId;
    @SerializedName(value = "image_url", alternate = {"imageUrl"})
    public String imageUrl;
    @SerializedName("is_primary")
    public Boolean isPrimary;
    @SerializedName("sort_order")
    public Integer sortOrder;
}
