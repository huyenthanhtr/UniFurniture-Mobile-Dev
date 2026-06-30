package com.unifurniture.mobile.data.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewMediaConfigDto {
    private Integer maxImages;
    private Integer maxVideos;
    private Integer maxTotalFiles;
    private Long maxFileSizeBytes;
    private Integer maxFileSizeMb;
    private List<String> acceptedTypes;

    public ReviewMediaConfigDto() {
        this.acceptedTypes = new ArrayList<>();
    }

    public Integer getMaxImages() {
        return maxImages;
    }

    public void setMaxImages(Integer maxImages) {
        this.maxImages = maxImages;
    }

    public Integer getMaxVideos() {
        return maxVideos;
    }

    public void setMaxVideos(Integer maxVideos) {
        this.maxVideos = maxVideos;
    }

    public Integer getMaxTotalFiles() {
        return maxTotalFiles;
    }

    public void setMaxTotalFiles(Integer maxTotalFiles) {
        this.maxTotalFiles = maxTotalFiles;
    }

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Integer getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(Integer maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public List<String> getAcceptedTypes() {
        return acceptedTypes;
    }

    public void setAcceptedTypes(List<String> acceptedTypes) {
        this.acceptedTypes = acceptedTypes != null ? acceptedTypes : new ArrayList<>();
    }
}
