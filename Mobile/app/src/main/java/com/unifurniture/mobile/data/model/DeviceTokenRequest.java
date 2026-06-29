package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/** Body for registering/unregistering an FCM device token with the backend. */
public class DeviceTokenRequest {

    @SerializedName("customer_id")
    public String customerId;

    public String token;

    public String platform;

    public DeviceTokenRequest(String customerId, String token) {
        this.customerId = customerId;
        this.token = token;
        this.platform = "android";
    }
}
