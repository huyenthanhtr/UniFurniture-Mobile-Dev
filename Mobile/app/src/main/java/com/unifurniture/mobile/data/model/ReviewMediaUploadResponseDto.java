package com.unifurniture.mobile.data.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewMediaUploadResponseDto {
    private List<String> urls;
    private List<String> images;
    private List<String> videos;

    public ReviewMediaUploadResponseDto() {
        this.urls = new ArrayList<>();
        this.images = new ArrayList<>();
        this.videos = new ArrayList<>();
    }

    public ReviewMediaUploadResponseDto(List<String> urls, List<String> images, List<String> videos) {
        this.urls = urls != null ? urls : new ArrayList<>();
        this.images = images != null ? images : new ArrayList<>();
        this.videos = videos != null ? videos : new ArrayList<>();
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls != null ? urls : new ArrayList<>();
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
        return "ReviewMediaUploadResponseDto{" +
                "urls=" + urls +
                ", images=" + images +
                ", videos=" + videos +
                '}';
    }
}
