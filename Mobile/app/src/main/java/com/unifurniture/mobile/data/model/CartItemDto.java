package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cart item as returned by server.
 *
 * IMPORTANT: Server populates variant_id as a nested object containing the
 * variant document with product_id also populated as a nested product.
 *
 * Shape from server:
 *   {
 *     _id,
 *     cart_id,
 *     variant_id: {          // populated ProductVariant
 *       _id, name, variant_name, sku, color, price, compare_at_price, stock_quantity,
 *       product_id: {        // populated Product
 *         _id, name, slug, thumbnail, thumbnail_url
 *       }
 *     },
 *     quantity,
 *     unit_price
 *   }
 */
public class CartItemDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("cart_id")
    public String cartId;
    @SerializedName("variant_id")
    public CartVariant variant;
    public Integer quantity;
    @SerializedName("unit_price")
    public Double unitPrice;

    // ── Nested populated variant ─────────────────────────────────────────────
    public static class CartVariant {
        @SerializedName("_id")
        public String id;
        public String name;
        @SerializedName("variant_name")
        public String variantName;
        public String sku;
        public String color;
        public Double price;
        @SerializedName("compare_at_price")
        public Double compareAtPrice;
        @SerializedName("stock_quantity")
        public Integer stockQuantity;
        @SerializedName("product_id")
        public CartProduct product;
    }

    // ── Nested populated product ─────────────────────────────────────────────
    public static class CartProduct {
        @SerializedName("_id")
        public String id;
        public String name;
        public String slug;
        public String thumbnail;
        @SerializedName("thumbnail_url")
        public String thumbnailUrl;

        public String getImageUrl() {
            if (thumbnail != null && !thumbnail.isEmpty()) return thumbnail;
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) return thumbnailUrl;
            return "";
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Get display name: variant's product name or variant name */
    public String getProductName() {
        if (variant != null && variant.product != null && variant.product.name != null) {
            return variant.product.name;
        }
        if (variant != null && variant.name != null) return variant.name;
        return "Sản phẩm";
    }

    /** Get display image URL */
    public String getImageUrl() {
        if (variant != null && variant.product != null) {
            return variant.product.getImageUrl();
        }
        return "";
    }

    /** Get variant color label */
    public String getColorLabel() {
        if (variant != null && variant.color != null && !variant.color.isEmpty()) {
            return variant.color;
        }
        if (variant != null && variant.variantName != null) return variant.variantName;
        return null;
    }

    /** Get unit price — prefer unitPrice field, fallback to variant price */
    public double getEffectivePrice() {
        if (unitPrice != null && unitPrice > 0) return unitPrice;
        if (variant != null && variant.price != null) return variant.price;
        return 0;
    }

    /** Total price = unit price × quantity */
    public double getTotalPrice() {
        int qty = quantity != null ? quantity : 1;
        return getEffectivePrice() * qty;
    }

    /** Get variant ID string */
    public String getVariantId() {
        return variant != null ? variant.id : null;
    }

    /** Get product ID string */
    public String getProductId() {
        return (variant != null && variant.product != null) ? variant.product.id : null;
    }
}
