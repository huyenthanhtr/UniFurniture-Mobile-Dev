package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderDto {
    @SerializedName("_id")
    private String id;
    @SerializedName("order_code")
    private String orderCode;
    @SerializedName("customer_id")
    private String customerId;
    private String status;
    @SerializedName("total_amount")
    private Double totalAmount;
    @SerializedName(value = "created_at", alternate = {"createdAt", "ordered_at"})
    private String createdAt;
    @SerializedName("shipping_name")
    private String shippingName;
    @SerializedName("shipping_phone")
    private String shippingPhone;
    @SerializedName("shipping_address")
    private String shippingAddress;
    @SerializedName("payment_method")
    private String paymentMethod;
    @SerializedName("payment_status")
    private String paymentStatus;
    @SerializedName("payment_summary")
    private PaymentSummaryDto paymentSummary;
    @SerializedName("tracking_code")
    private String trackingCode;
    @SerializedName(value = "details", alternate = {"items", "order_items_preview"})
    private List<OrderDetailDto> details;

    public OrderDto() {
    }

    public OrderDto(String id, String customerId, String status, Double totalAmount, String createdAt, String shippingName, String shippingPhone, String shippingAddress, String paymentMethod, String paymentStatus, String trackingCode, List<OrderDetailDto> details) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.trackingCode = trackingCode;
        this.details = details;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getShippingName() {
        return shippingName;
    }

    public void setShippingName(String shippingName) {
        this.shippingName = shippingName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentSummaryDto getPaymentSummary() {
        return paymentSummary;
    }

    public void setPaymentSummary(PaymentSummaryDto paymentSummary) {
        this.paymentSummary = paymentSummary;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public List<OrderDetailDto> getDetails() {
        return details;
    }

    public void setDetails(List<OrderDetailDto> details) {
        this.details = details;
    }

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

    @Override
    public String toString() {
        return "OrderDto{" +
                "id='" + id + '\'' +
                ", orderCode='" + orderCode + '\'' +
                ", customerId='" + customerId + '\'' +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", createdAt='" + createdAt + '\'' +
                ", shippingName='" + shippingName + '\'' +
                ", shippingPhone='" + shippingPhone + '\'' +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", trackingCode='" + trackingCode + '\'' +
                ", details=" + details +
                '}';
    }
}
