package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProductDto {
    @SerializedName("_id")
    public String id;
    public String name;
    public String slug;
    public String status;
    public String thumbnail;
    @SerializedName("thumbnail_url")
    public String thumbnailUrl;
    @SerializedName("min_price")
    public Double minPrice;
    @SerializedName("compare_at_price")
    public Double compareAtPrice;
    public Integer sold;
    public String sku;
    public String description;
    @SerializedName("short_description")
    public String shortDescription;
    @SerializedName("warranty_months")
    public Integer warrantyMonths;
    @SerializedName("category_id")
    public String categoryId;
    @SerializedName("collection_id")
    public String collectionId;
    @SerializedName("average_rating")
    public Double averageRating;

    // Helper: get best image URL
    public String getImageUrl() {
        if (thumbnail != null && !thumbnail.isEmpty()) return thumbnail;
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) return thumbnailUrl;
        return "";
    }

    // Helper: discount badge text
    public String getDiscountBadge() {
        if (minPrice != null && compareAtPrice != null && compareAtPrice > minPrice) {
            int pct = (int) Math.round((compareAtPrice - minPrice) / compareAtPrice * 100);
            return "-" + pct + "%";
        }
        return null;
    }
}
