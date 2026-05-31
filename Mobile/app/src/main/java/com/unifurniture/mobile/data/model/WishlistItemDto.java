package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Wishlist item — matches server's mapWishlistItem() output.
 *
 * Server stores: profile_id, product_id, product_slug, name, image_url, sale_price, price
 * (NOT customer_id, NOT a nested ProductDto)
 */
public class WishlistItemDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("profile_id")
    public String profileId;
    @SerializedName("account_name")
    public String accountName;
    @SerializedName("account_phone")
    public String accountPhone;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("product_slug")
    public String productSlug;
    public String name;
    @SerializedName("image_url")
    public String imageUrl;
    @SerializedName("sale_price")
    public Double salePrice;
    public Double price;
    public String createdAt;
    public String updatedAt;
}
