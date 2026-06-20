package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class OrderDetailDto {
    @SerializedName("_id")
    private String id;
    @SerializedName("order_id")
    private String orderId;
    @SerializedName("product_id")
    private String productId;
    @SerializedName("variant_id")
    private String variantId;
    private Integer quantity;
    @SerializedName("unit_price")
    private Double unitPrice;
    private Double total;
    @SerializedName("product_name")
    private String productName;
    @SerializedName("variant_name")
    private String variantName;
    @SerializedName("image_url")
    private String imageUrl;
    private ProductDto product;

    public OrderDetailDto() {
    }

    public OrderDetailDto(String id, String orderId, String productId, String variantId, Integer quantity, Double unitPrice, ProductDto product) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.product = product;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public ProductDto getProduct() {
        return product;
    }

    public void setProduct(ProductDto product) {
        this.product = product;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "OrderDetailDto{" +
                "id='" + id + '\'' +
                ", orderId='" + orderId + '\'' +
                ", productId='" + productId + '\'' +
                ", variantId='" + variantId + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", total=" + total +
                ", productName='" + productName + '\'' +
                ", variantName='" + variantName + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", product=" + product +
                '}';
    }
}
