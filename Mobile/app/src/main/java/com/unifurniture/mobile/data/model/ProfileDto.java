package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProfileDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("customer_id")
    public String customerId;
    public String name;
    public String email;
    public String phone;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public String gender;
    public String birthday;
}
