package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class PointTransactionDto {
    @SerializedName("_id")
    private String id;

    @SerializedName("profile_id")
    private String profileId;

    @SerializedName("order_id")
    private String orderId;

    private int points;
    private String type;
    private String note;

    @SerializedName("createdAt")
    private String createdAt;

    public PointTransactionDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
