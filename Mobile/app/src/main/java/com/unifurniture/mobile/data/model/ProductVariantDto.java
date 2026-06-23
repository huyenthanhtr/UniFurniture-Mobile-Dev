package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProductVariantDto {
    @SerializedName(value = "_id", alternate = {"id"})
    public String id;
    @SerializedName(value = "product_id", alternate = {"productId"})
    public Object productId;
    public String name;
    @SerializedName(value = "variant_name", alternate = {"variantName"})
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
    @SerializedName(value = "color", alternate = {"color_name", "colorName"})
    public String color;
    public Double price;
    @SerializedName(value = "compare_at_price", alternate = {"compareAtPrice"})
    public Double compareAtPrice;
    @SerializedName(value = "stock_quantity", alternate = {"stockQuantity"})
    public Integer stockQuantity;
    @SerializedName(value = "variant_status", alternate = {"status", "variantStatus"})
    public String variantStatus;
}
