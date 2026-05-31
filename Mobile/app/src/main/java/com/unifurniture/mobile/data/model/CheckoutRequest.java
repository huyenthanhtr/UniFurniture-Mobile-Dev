package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request body for POST /orders (createCheckoutOrder).
 *
 * Server expects items[] array (NOT cart_id). Each item has variant_id,
 * product_name, quantity, unit_price, etc.
 */
public class CheckoutRequest {
    @SerializedName("account_id")
    public String accountId;
    @SerializedName("shipping_name")
    public String shippingName;
    @SerializedName("shipping_phone")
    public String shippingPhone;
    @SerializedName("shipping_email")
    public String shippingEmail;
    @SerializedName("shipping_address")
    public String shippingAddress;
    public String province;
    public String district;
    @SerializedName("payment_method")
    public String paymentMethod;
    @SerializedName("shipping_method")
    public String shippingMethod;
    @SerializedName("coupon_code")
    public String couponCode;
    @SerializedName("coupon_discount")
    public Double couponDiscount;
    @SerializedName("total_amount")
    public Double totalAmount;
    @SerializedName("deposit_amount")
    public Double depositAmount;
    public List<CheckoutItem> items;

    public CheckoutRequest(String accountId,
                           String shippingName, String shippingPhone,
                           String shippingAddress, String paymentMethod,
                           List<CheckoutItem> items) {
        this.accountId = accountId;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }
}
