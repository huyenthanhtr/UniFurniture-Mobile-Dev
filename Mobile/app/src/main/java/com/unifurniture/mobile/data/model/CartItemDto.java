package com.unifurniture.mobile.data.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class CartItemDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("cart_id")
    public String cartId;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("variant_id")
    public Object variantIdRaw;
    public Integer quantity;
    @SerializedName("unit_price")
    public Double price;
    public ProductDto product;
    public ProductVariantDto variant;

    private transient boolean normalized;

    public String getVariantId() {
        if (variantIdRaw == null) {
            return null;
        }
        if (variantIdRaw instanceof String) {
            return (String) variantIdRaw;
        }
        if (variantIdRaw instanceof Map) {
            Object id = ((Map<?, ?>) variantIdRaw).get("_id");
            if (id != null) {
                return id.toString();
            }
            id = ((Map<?, ?>) variantIdRaw).get("id");
            if (id != null) {
                return id.toString();
            }
        }
        return null;
    }

    public ProductDto getProduct() {
        normalizeVariantData();
        return product;
    }

    public ProductVariantDto getVariant() {
        normalizeVariantData();
        return variant;
    }

    public double getUnitPrice() {
        if (price != null) {
            return price;
        }
        ProductVariantDto v = getVariant();
        return (v != null && v.price != null) ? v.price : 0;
    }

    public double getTotalPrice() {
        double unitPrice = getUnitPrice();
        int qty = quantity != null ? quantity : 1;
        return unitPrice * qty;
    }

    private void normalizeVariantData() {
        if (normalized) return;
        normalized = true;

        if (variantIdRaw instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) variantIdRaw;
            Gson gson = new Gson();
            String json = gson.toJson(rawMap);
            variant = gson.fromJson(json, ProductVariantDto.class);

            Object rawProduct = rawMap.get("product_id");
            if (rawProduct instanceof Map) {
                product = gson.fromJson(gson.toJson(rawProduct), ProductDto.class);
                if (product != null && variant != null) {
                    variant.productId = product.id;
                }
            } else if (rawProduct instanceof String) {
                if (variant != null) {
                    variant.productId = rawProduct.toString();
                }
            }
        }

        if (product == null && productId != null) {
            product = new ProductDto();
            product.id = productId;
        }
    }
}
