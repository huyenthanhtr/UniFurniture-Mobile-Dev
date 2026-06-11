package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("customer_id")
    public String customerId;
    public String status;
    @SerializedName("total_amount")
    public Double totalAmount;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("shipping_name")
    public String shippingName;
    @SerializedName("shipping_phone")
    public String shippingPhone;
    @SerializedName("shipping_address")
    public String shippingAddress;
    @SerializedName("payment_method")
    public String paymentMethod;
    @SerializedName("payment_status")
    public String paymentStatus;
    @SerializedName("tracking_code")
    public String trackingCode;
    public List<OrderDetailDto> details;

    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        switch (status) {
            case "pending":      return "Chờ xác nhận";
            case "confirmed":    return "Đã xác nhận";
            case "shipping":     return "Đang giao";
            case "delivered":    return "Đã giao";
            case "cancelled":    return "Đã huỷ";
            case "cancel_requested": return "Yêu cầu huỷ";
            default:             return status;
        }
    }
}
