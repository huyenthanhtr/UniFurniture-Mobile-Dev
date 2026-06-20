package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CustomerDto {
    @SerializedName("_id")
    public String id;
    public String phone;
    @SerializedName(value = "name", alternate = {"full_name"})
    public String name;
    public String email;
    public String status;

    public CustomerDto() {
    }

    public CustomerDto(String id, String phone, String name, String email, String status) {
        this.id = id;
        this.phone = phone;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "CustomerDto{" +
                "id='" + id + '\'' +
                ", phone='" + phone + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
