package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ProfileDto {
    @SerializedName("_id")
    public String id;
    public String phone;
    public String email;
    @SerializedName("password_hash")
    public String passwordHash;
    public String role;
    @SerializedName("account_status")
    public String accountStatus;
    @SerializedName("full_name")
    public String fullName;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public String gender;
    @SerializedName("date_of_birth")
    public String dateOfBirth;
    public String address;
    @SerializedName("customer_id")
    public String customerId;
    @SerializedName("loyalty_points_lifetime")
    public Integer loyaltyPointsLifetime;
    @SerializedName("membership_tier")
    public String membershipTier;

    /**
     * Helper: get display name (fallback to phone if full_name is empty)
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
        if (phone != null && !phone.trim().isEmpty()) return phone.trim();
        return "Khách hàng";
    }
}
