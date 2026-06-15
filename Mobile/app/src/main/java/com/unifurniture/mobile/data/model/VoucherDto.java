package com.unifurniture.mobile.data.model;

public class VoucherDto {
    public String code;
    public String name;
    public String description;
    public String discountType; // "percent" or "fixed"
    public double discountValue;
    public double minOrderValue;
    public double maxDiscountValue; // only applicable if discountType is "percent"
    public boolean isUsed;
    public String expirationDate;

    public VoucherDto() {
    }

    public VoucherDto(String code, String name, String description, String discountType,
                      double discountValue, double minOrderValue, double maxDiscountValue,
                      boolean isUsed, String expirationDate) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscountValue = maxDiscountValue;
        this.isUsed = isUsed;
        this.expirationDate = expirationDate;
    }
}
