package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class PricingDto {
    @SerializedName(value = "items_subtotal", alternate = {"subtotal", "subtotal_amount", "items_total"})
    private Double itemsSubtotal;
    @SerializedName(value = "shipping_fee", alternate = {"shippingFee", "delivery_fee", "deliveryFee"})
    private Double shippingFee;
    @SerializedName(value = "discount_amount", alternate = {"coupon_discount", "discount", "voucher_discount"})
    private Double discountAmount;
    @SerializedName(value = "coupon_code", alternate = {"voucher_code"})
    private String couponCode;
    @SerializedName(value = "grand_total", alternate = {"total_amount", "total"})
    private Double grandTotal;

    public Double getItemsSubtotal() {
        return itemsSubtotal;
    }

    public Double getShippingFee() {
        return shippingFee;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public Double getGrandTotal() {
        return grandTotal;
    }
}
