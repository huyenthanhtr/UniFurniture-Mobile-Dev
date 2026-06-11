package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class CollectionDto {
    @SerializedName("_id")
    public String id;
    public String name;
    public String slug;
    public String status;
    @SerializedName("banner_url")
    public String bannerUrl;
}
