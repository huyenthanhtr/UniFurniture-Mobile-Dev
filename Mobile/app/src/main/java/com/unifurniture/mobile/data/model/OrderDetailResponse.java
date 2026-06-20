package com.unifurniture.mobile.data.model;

import java.util.List;

public class OrderDetailResponse {
    private OrderDto order;
    private List<OrderDetailDto> items;

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

    @Override
    public String toString() {
        return "OrderDetailResponse{" +
                "order=" + order +
                ", items=" + items +
                '}';
    }
}
