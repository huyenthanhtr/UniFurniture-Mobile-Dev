package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Review item — matches server review controller output.
 * Reviews are linked to order_detail_id (not product_id directly).
 */
public class ReviewDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("order_detail_id")
    public String orderDetailId;
    public Integer rating;
    public String content;
    public List<String> images;
    public List<String> videos;
    public String status;
    public String createdAt;
    public String customerName;
    public String productName;
    public ReviewReplyDto reply;

    public static class ReviewReplyDto {
        public String content;
        public String repliedAt;
    }
}
