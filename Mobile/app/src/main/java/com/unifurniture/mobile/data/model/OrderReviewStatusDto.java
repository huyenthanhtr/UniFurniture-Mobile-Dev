package com.unifurniture.mobile.data.model;

import java.util.ArrayList;
import java.util.List;

public class OrderReviewStatusDto {
    private String orderId;
    private List<String> reviewedDetailIds;
    private Boolean hasReviewed;
    private List<OrderReviewItemDto> items;

    public OrderReviewStatusDto() {
        this.reviewedDetailIds = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    public OrderReviewStatusDto(String orderId, List<String> reviewedDetailIds, Boolean hasReviewed, List<OrderReviewItemDto> items) {
        this.orderId = orderId;
        this.reviewedDetailIds = reviewedDetailIds != null ? reviewedDetailIds : new ArrayList<>();
        this.hasReviewed = hasReviewed;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<String> getReviewedDetailIds() {
        return reviewedDetailIds;
    }

    public void setReviewedDetailIds(List<String> reviewedDetailIds) {
        this.reviewedDetailIds = reviewedDetailIds != null ? reviewedDetailIds : new ArrayList<>();
    }

    public Boolean getHasReviewed() {
        return hasReviewed;
    }

    public void setHasReviewed(Boolean hasReviewed) {
        this.hasReviewed = hasReviewed;
    }

    public List<OrderReviewItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderReviewItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "OrderReviewStatusDto{" +
                "orderId='" + orderId + '\'' +
                ", reviewedDetailIds=" + reviewedDetailIds +
                ", hasReviewed=" + hasReviewed +
                ", items=" + items +
                '}';
    }
}
