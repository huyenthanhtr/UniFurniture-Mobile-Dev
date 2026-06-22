package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CustomerAddressDto {
    @SerializedName("_id")
    public String id;
    
    @SerializedName("customer_address_name")
    public String fullName;
    
    @SerializedName("address_phone")
    public String phone;
    
    @SerializedName("province")
    public String province;
    
    @SerializedName("district")
    public String district;
    
    @SerializedName("address_line")
    public String address;
    
    @SerializedName("is_default")
    public boolean isDefault;

    public CustomerAddressDto() {
    }

    public CustomerAddressDto(String id, String fullName, String phone, String province, String district, String address, boolean isDefault) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.province = province;
        this.district = district;
        this.address = address;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getDisplayAddress() {
        return String.format("%s, %s, %s", safe(address), safe(district), safe(province));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    @Override
    public String toString() {
        return getDisplayAddress();
    }
}
