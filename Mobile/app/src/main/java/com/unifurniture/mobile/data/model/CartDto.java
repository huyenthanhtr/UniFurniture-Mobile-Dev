package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CartDto {
    @SerializedName("_id")
    public String id;
    @SerializedName("customer_id")
    public String customerId;
    public List<CartItemDto> items;
    public Double total;
}
