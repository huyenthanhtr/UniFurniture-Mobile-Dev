package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("product_id")
    public String productId;
    @SerializedName("customer_id")
    public String customerId;
    public Integer rating;
    public String content;
    public List<String> images;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("customer_name")
    public String customerName;
    public ReviewReplyDto reply;

    public static class ReviewReplyDto {
        public String content;
        @SerializedName("replied_at")
        public String repliedAt;
    }
}
