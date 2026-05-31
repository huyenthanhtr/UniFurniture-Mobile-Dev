package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Order detail — matches server OrderDetail model.
 * Includes product_name, variant_name, sku, image_url from server snapshot.
 */
public class OrderDetailDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("order_id")
    public String orderId;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("variant_id")
    public String variantId;
    @SerializedName("product_name")
    public String productName;
    @SerializedName("variant_name")
    public String variantName;
    public String sku;
    public Integer quantity;
    @SerializedName("unit_price")
    public Double unitPrice;
    public Double total;
    @SerializedName("image_url")
    public String imageUrl;

    // legacy field — some old responses may include a nested product
    public ProductDto product;

    /**
     * Get display name — prefer server-snapshot product_name
     */
    public String getDisplayName() {
        if (productName != null && !productName.isEmpty()) return productName;
        if (product != null && product.name != null) return product.name;
        return "Sản phẩm";
    }

    /**
     * Get display image — prefer server-snapshot image_url
     */
    public String getDisplayImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) return imageUrl;
        if (product != null) return product.getImageUrl();
        return "";
    }
}
