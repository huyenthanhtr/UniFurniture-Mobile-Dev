package com.unifurniture.mobile.data.model;

public class NotificationDto {
    public String id;
    public String title;
    public String content;
    public String type; // "order" or "account"
    public long timestamp;
    public boolean isRead;
    public String orderId;

    public NotificationDto() {
    }

    public NotificationDto(String id, String title, String content, String type, long timestamp, boolean isRead, String orderId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.orderId = orderId;
    }
}
