package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderDetailResponse {
    private OrderDto order;
    private List<OrderDetailDto> items;
    private List<PaymentDto> payments;
    @SerializedName("payment_summary")
    private PaymentSummaryDto paymentSummary;

    public OrderDetailResponse() {
    }

    public OrderDetailResponse(OrderDto order, List<OrderDetailDto> items) {
        this.order = order;
        this.items = items;
    }

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
        this.order = order;
    }

    public List<OrderDetailDto> getItems() {
        return items;
    }

    public void setItems(List<OrderDetailDto> items) {
        this.items = items;
    }

    public List<PaymentDto> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentDto> payments) {
        this.payments = payments;
    }

    public PaymentSummaryDto getPaymentSummary() {
        return paymentSummary;
    }

    public void setPaymentSummary(PaymentSummaryDto paymentSummary) {
        this.paymentSummary = paymentSummary;
    }

    @Override
    public String toString() {
        return "OrderDetailResponse{" +
                "order=" + order +
                ", items=" + items +
                ", payments=" + payments +
                ", paymentSummary=" + paymentSummary +
                '}';
    }
}
