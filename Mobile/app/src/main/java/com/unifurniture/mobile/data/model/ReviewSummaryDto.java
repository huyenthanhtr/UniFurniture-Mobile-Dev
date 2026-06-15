package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewSummaryDto {
    public String productId;
    public int totalReviews;
    public double averageRating;
    public List<ReviewDto> items;
}
