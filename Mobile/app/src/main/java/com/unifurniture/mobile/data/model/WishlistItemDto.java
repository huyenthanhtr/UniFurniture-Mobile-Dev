package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class WishlistItemDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("customer_id")
    public String customerId;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("product_slug")
    public String productSlug;
    @SerializedName("image_url")
    public String imageUrl;
    @SerializedName("sale_price")
    public Double salePrice;
    @SerializedName("price")
    public Double price;
    public String name;
    public ProductDto product;

    public ProductDto getProduct() {
        if (product != null) {
            return product;
        }
        if (productId == null && name == null && imageUrl == null) {
            return null;
        }
        ProductDto dto = new ProductDto();
        dto.id = productId;
        dto.slug = productSlug;
        dto.name = name;
        dto.thumbnailUrl = imageUrl;
        dto.minPrice = salePrice != null ? salePrice : price;
        dto.compareAtPrice = price != null ? price : null;
        return dto;
    }
}
