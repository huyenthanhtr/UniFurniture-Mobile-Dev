package com.unifurniture.mobile.data.model;

public class NotificationDto {
    public String id;
    public String title;
    public String content;
    public String type; // "order" or "account"
    public String eventKey;
    public String couponCode;
    public long timestamp;
    public boolean isRead;
    public String orderId;

    public NotificationDto() {
    }

    public NotificationDto(String id, String title, String content, String type, String eventKey, String couponCode, long timestamp, boolean isRead, String orderId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.eventKey = eventKey;
        this.couponCode = couponCode;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.orderId = orderId;
    }
}
