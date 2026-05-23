package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CheckoutRequest {
    @SerializedName("customer_id")
    public String customerId;
    @SerializedName("cart_id")
    public String cartId;
    @SerializedName("shipping_name")
    public String shippingName;
    @SerializedName("shipping_phone")
    public String shippingPhone;
    @SerializedName("shipping_address")
    public String shippingAddress;
    @SerializedName("payment_method")
    public String paymentMethod = "cod";
    @SerializedName("coupon_code")
    public String couponCode;

    public CheckoutRequest(String customerId, String cartId,
                           String shippingName, String shippingPhone,
                           String shippingAddress, String paymentMethod) {
        this.customerId = customerId;
        this.cartId = cartId;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }
}
