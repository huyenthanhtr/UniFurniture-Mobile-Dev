package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class LoyaltyDto {
    @SerializedName("loyalty_points_lifetime")
    private int loyaltyPointsLifetime;

    @SerializedName("membership_tier")
    private String membershipTier;

    @SerializedName("membership_tier_label")
    private String membershipTierLabel;

    @SerializedName("next_tier")
    private String nextTier;

    @SerializedName("next_tier_label")
    private String nextTierLabel;

    @SerializedName("points_to_next_tier")
    private int pointsToNextTier;

    public LoyaltyDto() {
    }

    public int getLoyaltyPointsLifetime() {
        return loyaltyPointsLifetime;
    }

    public void setLoyaltyPointsLifetime(int loyaltyPointsLifetime) {
        this.loyaltyPointsLifetime = loyaltyPointsLifetime;
    }

    public String getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(String membershipTier) {
        this.membershipTier = membershipTier;
    }

    public String getMembershipTierLabel() {
        return membershipTierLabel;
    }

    public void setMembershipTierLabel(String membershipTierLabel) {
        this.membershipTierLabel = membershipTierLabel;
    }

    public String getNextTier() {
        return nextTier;
    }

    public void setNextTier(String nextTier) {
        this.nextTier = nextTier;
    }

    public String getNextTierLabel() {
        return nextTierLabel;
    }

    public void setNextTierLabel(String nextTierLabel) {
        this.nextTierLabel = nextTierLabel;
    }

    public int getPointsToNextTier() {
        return pointsToNextTier;
    }

    public void setPointsToNextTier(int pointsToNextTier) {
        this.pointsToNextTier = pointsToNextTier;
    }
}
