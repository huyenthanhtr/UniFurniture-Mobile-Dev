package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ReviewSubmissionItemDto {
    @SerializedName(value = "order_detail_id", alternate = {"orderDetailId"})
    private String orderDetailId;
    private Integer rating;
    private String content;
    private List<String> images;
    private List<String> videos;

    public ReviewSubmissionItemDto() {
        this.images = new ArrayList<>();
        this.videos = new ArrayList<>();
    }

    public ReviewSubmissionItemDto(String orderDetailId, Integer rating, String content, List<String> images, List<String> videos) {
        this.orderDetailId = orderDetailId;
        this.rating = rating;
        this.content = content;
        this.images = images != null ? images : new ArrayList<>();
        this.videos = videos != null ? videos : new ArrayList<>();
    }

    public String getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(String orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images != null ? images : new ArrayList<>();
    }

    public List<String> getVideos() {
        return videos;
    }

    public void setVideos(List<String> videos) {
        this.videos = videos != null ? videos : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ReviewSubmissionItemDto{" +
                "orderDetailId='" + orderDetailId + '\'' +
                ", rating=" + rating +
                ", content='" + content + '\'' +
                ", images=" + images +
                ", videos=" + videos +
                '}';
    }
}
