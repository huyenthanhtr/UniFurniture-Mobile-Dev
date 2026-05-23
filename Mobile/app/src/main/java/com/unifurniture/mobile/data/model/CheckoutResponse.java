package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CheckoutResponse {
    public OrderDto order;
    public String message;
    @SerializedName("payment_url")
    public String paymentUrl;
}
