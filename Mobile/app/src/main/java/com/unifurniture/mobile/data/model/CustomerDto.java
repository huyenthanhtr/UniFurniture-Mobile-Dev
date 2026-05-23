package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CustomerDto {
    @SerializedName("_id")
    public String id;
    public String phone;
    public String name;
    public String email;
    public String status;
}
