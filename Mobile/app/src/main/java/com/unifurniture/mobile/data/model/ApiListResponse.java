package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// ─── Generic List Wrapper ─────────────────────────────────────────────────────
// Mirrors server's ApiListResponse<T>
public class ApiListResponse<T> {
    public int page;
    public int limit;
    public int total;
    public int totalPages;
    public List<T> items;

    public List<T> getData() {
        return items;
    }

    public void setData(List<T> data) {
        this.items = data;
    }
}
