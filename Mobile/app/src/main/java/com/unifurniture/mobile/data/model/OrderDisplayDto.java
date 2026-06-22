package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class OrderDisplayDto {
    @SerializedName("receiver_name")
    private String receiverName;
    private String phone;
    private String email;
    private String address;
    @SerializedName("customer_type")
    private String customerType;
    @SerializedName("has_account")
    private Boolean hasAccount;

    public String getReceiverName() {
        return receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getCustomerType() {
        return customerType;
    }

    public Boolean getHasAccount() {
        return hasAccount;
    }
}
