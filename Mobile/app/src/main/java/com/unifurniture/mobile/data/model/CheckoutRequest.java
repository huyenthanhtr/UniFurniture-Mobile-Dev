package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CheckoutRequest {
    @SerializedName("account_id")
    public String accountId;
    @SerializedName("customer_id")
    public String customerId; // kept for legacy reference
    @SerializedName("cart_id")
    public String cartId;
    @SerializedName("shipping_name")
    public String shippingName;
    @SerializedName("shipping_phone")
    public String shippingPhone;
    @SerializedName("shipping_email")
    public String shippingEmail;
    @SerializedName("shipping_address")
    public String shippingAddress;
    @SerializedName("province")
    public String province;
    @SerializedName("district")
    public String district;
    @SerializedName("payment_method")
    public String paymentMethod = "COD";
    @SerializedName("shipping_method")
    public String shippingMethod;
    @SerializedName("coupon_code")
    public String couponCode;
    @SerializedName("coupon_discount")
    public double couponDiscount;
    @SerializedName("total_amount")
    public double totalAmount;
    @SerializedName("deposit_amount")
    public double depositAmount;
    @SerializedName("items")
    public List<Item> items;

    public static class Item {
        @SerializedName("product_id")
        public String productId;
        @SerializedName("variant_id")
        public String variantId;
        public int quantity;
        @SerializedName("unit_price")
        public double unitPrice;
        public String name;
        @SerializedName("variant_name")
        public String variantName;
        public String sku;

        public Item(String productId, String variantId, int quantity, double unitPrice,
                    String name, String variantName, String sku) {
            this.productId = productId;
            this.variantId = variantId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.name = name;
            this.variantName = variantName;
            this.sku = sku;
        }
    }

    public CheckoutRequest(String accountId, String cartId,
                           String shippingName, String shippingPhone,
                           String shippingAddress, String paymentMethod) {
        this.accountId = accountId;
        this.customerId = null;
        this.cartId = cartId;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }
}
