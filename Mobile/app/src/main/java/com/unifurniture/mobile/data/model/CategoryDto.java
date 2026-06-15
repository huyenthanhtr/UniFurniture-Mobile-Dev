package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CategoryDto {
    @SerializedName("_id")
    public String id;
    public String name;
    public String slug;
    public String room;
    public String status;
    @SerializedName("image_url")
    public String imageUrl;
}
