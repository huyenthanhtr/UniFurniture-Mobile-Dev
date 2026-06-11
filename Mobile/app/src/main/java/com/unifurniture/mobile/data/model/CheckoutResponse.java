package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CheckoutResponse {
    @SerializedName("order_id")
    public String orderId;
    @SerializedName("order_code")
    public String orderCode;
    public String message;
    @SerializedName("payment_url")
    public String paymentUrl;
}
