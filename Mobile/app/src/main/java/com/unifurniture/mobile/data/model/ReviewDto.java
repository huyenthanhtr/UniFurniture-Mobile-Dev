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

    public static class ReviewReplyDto {
        public String content;
        public String repliedAt;
    }
}
