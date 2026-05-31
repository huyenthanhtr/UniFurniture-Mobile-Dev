package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Order document — matches server Order model.
 * All 9 statuses are mapped in getStatusLabel().
 */
public class OrderDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("order_code")
    public String orderCode;
    @SerializedName("customer_id")
    public String customerId;
    @SerializedName("account_id")
    public String accountId;
    public String status;
    @SerializedName("total_amount")
    public Double totalAmount;
    @SerializedName("deposit_amount")
    public Double depositAmount;
    @SerializedName("ordered_at")
    public String orderedAt;
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
    @SerializedName("payment_status")
    public String paymentStatus;
    @SerializedName("shipping_method")
    public String shippingMethod;
    @SerializedName("tracking_code")
    public String trackingCode;
    @SerializedName("is_installed")
    public Boolean isInstalled;
    @SerializedName("coupon_code")
    public String couponCode;
    @SerializedName("coupon_discount")
    public Double couponDiscount;
    @SerializedName("cancel_reason")
    public String cancelReason;
    @SerializedName("cancel_phone")
    public String cancelPhone;
    public String note;
    public List<OrderDetailDto> details;
    public String createdAt;
    public String updatedAt;

    /**
     * Maps server status values to Vietnamese labels.
     * Server uses 9 distinct statuses:
     *   pending, processing, confirmed, shipping, delivered,
     *   completed, cancelled, exchanged, cancel_pending
     */
    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        switch (status) {
            case "pending":         return "Chờ xác nhận";
            case "processing":      return "Đang xử lý";
            case "confirmed":       return "Đã xác nhận";
            case "shipping":        return "Đang giao";
            case "delivered":       return "Đã giao";
            case "completed":       return "Hoàn tất";
            case "cancelled":       return "Đã huỷ";
            case "exchanged":       return "Đã đổi hàng";
            case "cancel_pending":  return "Chờ xác nhận huỷ";
            default:                return status;
        }
    }

    /**
     * Get display date — prefer orderedAt, fallback to createdAt
     */
    public String getDisplayDate() {
        String raw = orderedAt != null ? orderedAt : createdAt;
        if (raw == null) return "";
        return raw.substring(0, Math.min(10, raw.length()));
    }

    /**
     * Get display order identifier — prefer order_code, fallback to last 8 chars of _id
     */
    public String getDisplayCode() {
        if (orderCode != null && !orderCode.isEmpty()) return orderCode;
        if (id != null) return id.substring(Math.max(0, id.length() - 8));
        return "";
    }
}
