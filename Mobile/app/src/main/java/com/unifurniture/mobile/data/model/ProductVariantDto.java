package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProductVariantDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("product_id")
    public String productId;
    public String name;
    @SerializedName("variant_name")
    public String variantName;
    public String label;
    public String sku;
    public String color;
    public Double price;
    @SerializedName("compare_at_price")
    public Double compareAtPrice;
    @SerializedName("stock_quantity")
    public Integer stockQuantity;
    @SerializedName("variant_status")
    public String variantStatus;
}
