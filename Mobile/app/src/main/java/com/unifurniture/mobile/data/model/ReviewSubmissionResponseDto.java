package com.unifurniture.mobile.data.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewSubmissionResponseDto {
    private String message;
    private Integer createdCount;
    private List<String> createdIds;
    private Integer rewardedReviewCount;
    private Integer rewardedPoints;
    private List<String> reviewedDetailIds;

    public ReviewSubmissionResponseDto() {
        this.createdIds = new ArrayList<>();
        this.reviewedDetailIds = new ArrayList<>();
    }

    public ReviewSubmissionResponseDto(String message, Integer createdCount, List<String> createdIds, Integer rewardedReviewCount, Integer rewardedPoints, List<String> reviewedDetailIds) {
        this.message = message;
        this.createdCount = createdCount;
        this.createdIds = createdIds != null ? createdIds : new ArrayList<>();
        this.rewardedReviewCount = rewardedReviewCount;
        this.rewardedPoints = rewardedPoints;
        this.reviewedDetailIds = reviewedDetailIds != null ? reviewedDetailIds : new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public List<String> getCreatedIds() {
        return createdIds;
    }

    public void setCreatedIds(List<String> createdIds) {
        this.createdIds = createdIds != null ? createdIds : new ArrayList<>();
    }

    public Integer getRewardedReviewCount() {
        return rewardedReviewCount;
    }

    public void setRewardedReviewCount(Integer rewardedReviewCount) {
        this.rewardedReviewCount = rewardedReviewCount;
    }

    public Integer getRewardedPoints() {
        return rewardedPoints;
    }

    public void setRewardedPoints(Integer rewardedPoints) {
        this.rewardedPoints = rewardedPoints;
    }

    public List<String> getReviewedDetailIds() {
        return reviewedDetailIds;
    }

    public void setReviewedDetailIds(List<String> reviewedDetailIds) {
        this.reviewedDetailIds = reviewedDetailIds != null ? reviewedDetailIds : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ReviewSubmissionResponseDto{" +
                "message='" + message + '\'' +
                ", createdCount=" + createdCount +
                ", createdIds=" + createdIds +
                ", rewardedReviewCount=" + rewardedReviewCount +
                ", rewardedPoints=" + rewardedPoints +
                ", reviewedDetailIds=" + reviewedDetailIds +
                '}';
    }
}
