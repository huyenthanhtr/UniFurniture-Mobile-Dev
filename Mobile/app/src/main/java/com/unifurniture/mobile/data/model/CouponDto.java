package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CouponDto {
    public String code;

    @SerializedName("discount_type")
    public String discountType; // "percent" or "fixed"

    @SerializedName("discount_value")
    public double discountValue;

    @SerializedName("max_discount_amount")
    public Double maxDiscountAmount;

    @SerializedName("min_order_value")
    public double minOrderValue;

    @SerializedName("total_limit")
    public int totalLimit;

    public int used;

    @SerializedName("start_at")
    public String startAt;

    @SerializedName("end_at")
    public String endAt;

    public String status;
}
