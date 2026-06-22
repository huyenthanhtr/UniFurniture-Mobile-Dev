package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class PostDto {
    @SerializedName("_id")
    private String id;
    private String title;
    private String slug;
    private String caption;
    private String content;
    @SerializedName("thumbnail_url")
    private String thumbnailUrl;
    @SerializedName("post_category")
    private String category;
    private String status;
    @SerializedName(value = "published_at", alternate = {"publishedAt", "createdAt"})
    private String publishedAt;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getCaption() {
        return caption;
    }

    public String getContent() {
        return content;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public String getPublishedAt() {
        return publishedAt;
    }
}
