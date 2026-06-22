package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class PaymentSummaryDto {
    private String method;
    private String status;
    private Integer count;
    @SerializedName("paid_total")
    private Double paidTotal;
    @SerializedName("deposit_amount")
    private Double depositAmount;
    @SerializedName("deposit_paid_total")
    private Double depositPaidTotal;
    @SerializedName("has_deposit_paid")
    private Boolean hasDepositPaid;
    @SerializedName("has_full_paid")
    private Boolean hasFullPaid;
    @SerializedName("has_remaining_paid")
    private Boolean hasRemainingPaid;
    @SerializedName("latest_paid_type")
    private String latestPaidType;
    @SerializedName("total_amount")
    private Double totalAmount;

    public String getMethod() {
        return method;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCount() {
        return count;
    }

    public Double getPaidTotal() {
        return paidTotal;
    }

    public Double getDepositAmount() {
        return depositAmount;
    }

    public Double getDepositPaidTotal() {
        return depositPaidTotal;
    }

    public Boolean getHasDepositPaid() {
        return hasDepositPaid;
    }

    public Boolean getHasFullPaid() {
        return hasFullPaid;
    }

    public Boolean getHasRemainingPaid() {
        return hasRemainingPaid;
    }

    public String getLatestPaidType() {
        return latestPaidType;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }
}
