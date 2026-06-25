package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewDto {
    @SerializedName("_id")
    public String id;
    public String productId;
    public String customerId;
    public Integer rating;
    public String content;
    public List<String> images;
    public String createdAt;
    public String customerName;
    public ReviewReplyDto reply;

    // Client-only state for the "Translate / See Original" toggle (not sent to / from the server).
    public transient String translatedContent;
    public transient boolean showingTranslation;

    public ReviewDto() {
    }

    public ReviewDto(String id, String productId, String customerId, Integer rating, String content, List<String> images, String createdAt, String customerName, ReviewReplyDto reply) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.rating = rating;
        this.content = content;
        this.images = images;
        this.createdAt = createdAt;
        this.customerName = customerName;
        this.reply = reply;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public ReviewReplyDto getReply() {
        return reply;
    }

    public void setReply(ReviewReplyDto reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return "ReviewDto{" +
                "id='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", rating=" + rating +
                ", content='" + content + '\'' +
                ", images=" + images +
                ", createdAt='" + createdAt + '\'' +
                ", customerName='" + customerName + '\'' +
                ", reply=" + reply +
                '}';
    }

    public static class ReviewReplyDto {
        public String content;
        public String repliedAt;

        public ReviewReplyDto() {
        }

        public ReviewReplyDto(String content, String repliedAt) {
            this.content = content;
            this.repliedAt = repliedAt;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRepliedAt() {
            return repliedAt;
        }

        public void setRepliedAt(String repliedAt) {
            this.repliedAt = repliedAt;
        }

        @Override
        public String toString() {
            return "ReviewReplyDto{" +
                    "content='" + content + '\'' +
                    ", repliedAt='" + repliedAt + '\'' +
                    '}';
        }
    }
}
