package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProfileDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("customer_id")
    public String customerId;
    @SerializedName(value = "name", alternate = {"full_name"})
    public String name;
    public String email;
    public String phone;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public String gender;
    @SerializedName(value = "birthday", alternate = {"date_of_birth"})
    public String birthday;

    public ProfileDto() {
    }

    public ProfileDto(String id, String customerId, String name, String email, String phone, String avatarUrl, String gender, String birthday) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.gender = gender;
        this.birthday = birthday;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    @Override
    public String toString() {
        return "ProfileDto{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", gender='" + gender + '\'' +
                ", birthday='" + birthday + '\'' +
                '}';
    }
}
