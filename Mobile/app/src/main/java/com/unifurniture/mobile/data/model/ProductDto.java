package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

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
    public java.util.List<ColorInfo> colors;

    public static class ColorInfo {
        public String name;
        public Double price;
        @SerializedName("originalPrice")
        public Double originalPrice;
        public String imageUrl;
    }

    /** compare_at_price có thể nằm trên product hoặc trong colors array từ variant */
    public Double getEffectiveCompareAtPrice() {
        if (compareAtPrice != null && compareAtPrice > 0) return compareAtPrice;
        if (colors != null) {
            for (ColorInfo c : colors) {
                if (c.originalPrice != null && c.originalPrice > 0) return c.originalPrice;
            }
        }
        return null;
    }

    // Helper: get best image URL
    public String getImageUrl() {
        String url = "";
        if (thumbnail != null && !thumbnail.isEmpty()) {
            url = thumbnail;
        } else if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            url = thumbnailUrl;
        }
        
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        return url;
    }

    public String getTitle() {
        return name;
    }

    public List<ImageDto> getImages() {
        List<ImageDto> images = new ArrayList<>();
        String url = getImageUrl();
        if (url != null && !url.isEmpty()) {
            images.add(new ImageDto(url));
        }
        return images;
    }

    // Helper: discount badge text
    public String getDiscountBadge() {
        if (minPrice != null && compareAtPrice != null && compareAtPrice > minPrice) {
            int pct = (int) Math.round((compareAtPrice - minPrice) / compareAtPrice * 100);
            return "-" + pct + "%";
        }
        return null;
    }

    public static class ImageDto {
        private final String url;

        public ImageDto(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
}
