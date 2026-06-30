package com.unifurniture.mobile.data.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewSubmissionRequestDto {
    private String orderId;
    private String reviewer_account_id;
    private List<ReviewSubmissionItemDto> reviews;

    public ReviewSubmissionRequestDto() {
        this.reviews = new ArrayList<>();
    }

    public ReviewSubmissionRequestDto(String orderId, String reviewerAccountId, List<ReviewSubmissionItemDto> reviews) {
        this.orderId = orderId;
        this.reviewer_account_id = reviewerAccountId;
        this.reviews = reviews != null ? reviews : new ArrayList<>();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReviewer_account_id() {
        return reviewer_account_id;
    }

    public void setReviewer_account_id(String reviewer_account_id) {
        this.reviewer_account_id = reviewer_account_id;
    }

    public List<ReviewSubmissionItemDto> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewSubmissionItemDto> reviews) {
        this.reviews = reviews != null ? reviews : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ReviewSubmissionRequestDto{" +
                "orderId='" + orderId + '\'' +
                ", reviewer_account_id='" + reviewer_account_id + '\'' +
                ", reviews=" + reviews +
                '}';
    }
}
