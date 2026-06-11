package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProductVariantDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("product_id")
    public Object productId;
    public String name;
    @SerializedName("variant_name")
    public String variantName;
    public String label;
    public String sku;

    public String getProductId() {
        if (productId == null) {
            return null;
        }
        if (productId instanceof String) {
            return (String) productId;
        }
        if (productId instanceof java.util.Map) {
            Object id = ((java.util.Map<?, ?>) productId).get("_id");
            if (id != null) return id.toString();
            id = ((java.util.Map<?, ?>) productId).get("id");
            if (id != null) return id.toString();
        }
        return null;
    }
    public String color;
    public Double price;
    @SerializedName("compare_at_price")
    public Double compareAtPrice;
    @SerializedName("stock_quantity")
    public Integer stockQuantity;
    @SerializedName("variant_status")
    public String variantStatus;
}
