package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class PaymentDto {
    @SerializedName("_id")
    private String id;
    private String type;
    private String method;
    private Double amount;
    private String status;
    @SerializedName("transaction_id")
    private String transactionId;
    @SerializedName("paid_at")
    private String paidAt;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMethod() {
        return method;
    }

    public Double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPaidAt() {
        return paidAt;
    }
}
