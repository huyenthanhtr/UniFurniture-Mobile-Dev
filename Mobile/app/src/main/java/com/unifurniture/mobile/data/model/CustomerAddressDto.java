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

    @Override
    public String toString() {
        return String.format("%s, %s, %s", address, district, province);
    }
}
