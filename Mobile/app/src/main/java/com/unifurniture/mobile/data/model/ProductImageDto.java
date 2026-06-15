package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProductImageDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("variant_id")
    public String variantId;
    @SerializedName("image_url")
    public String imageUrl;
    @SerializedName("is_primary")
    public Boolean isPrimary;
    @SerializedName("sort_order")
    public Integer sortOrder;
}
