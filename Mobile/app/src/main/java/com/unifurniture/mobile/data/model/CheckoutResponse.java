package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from POST /orders (createCheckoutOrder).
 *
 * Server returns: { message, order_id, order_code, total_amount, deposit_amount, status }
 */
public class CheckoutResponse {
    public String message;
    @SerializedName("order_id")
    public String orderId;
    @SerializedName("order_code")
    public String orderCode;
    @SerializedName("total_amount")
    public Double totalAmount;
    @SerializedName("deposit_amount")
    public Double depositAmount;
    public String status;
}
