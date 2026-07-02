package com.unifurniture.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReviewDto {
    @SerializedName("_id")
    public String id;
    @SerializedName(value = "order_detail_id", alternate = {"orderDetailId"})
    public String orderDetailId;
    public String productId;
    public String productSlug;
    public String productName;
    public String productImageUrl;
    public String customerId;
    public String orderCode;
    public Integer rating;
    public String content;
    public List<String> images;
    public List<String> videos;
    public String status;
    public String createdAt;
    public String customerName;
    public ReviewReplyDto reply;

    // Client-only state for the "Translate / See Original" toggle (not sent to / from the server).
    public transient String translatedContent;
    public transient boolean showingTranslation;

    public ReviewDto() {
    }

    public ReviewDto(String id, String productId, String customerId, Integer rating, String content, List<String> images, String createdAt, String customerName, ReviewReplyDto reply) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.rating = rating;
        this.content = content;
        this.images = images;
        this.createdAt = createdAt;
        this.customerName = customerName;
        this.reply = reply;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductSlug() {
        return productSlug;
    }

    public void setProductSlug(String productSlug) {
        this.productSlug = productSlug;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
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
        this.images = images;
    }

    public List<String> getVideos() {
        return videos;
    }

    public void setVideos(List<String> videos) {
        this.videos = videos;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(String orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public ReviewReplyDto getReply() {
        return reply;
    }

    public void setReply(ReviewReplyDto reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return "ReviewDto{" +
                "id='" + id + '\'' +
                ", orderDetailId='" + orderDetailId + '\'' +
                ", productId='" + productId + '\'' +
                ", productSlug='" + productSlug + '\'' +
                ", productName='" + productName + '\'' +
                ", productImageUrl='" + productImageUrl + '\'' +
                ", customerId='" + customerId + '\'' +
                ", orderCode='" + orderCode + '\'' +
                ", rating=" + rating +
                ", content='" + content + '\'' +
                ", images=" + images +
                ", videos=" + videos +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", customerName='" + customerName + '\'' +
                ", reply=" + reply +
                '}';
    }

    public static class ReviewReplyDto {
        public String content;
        public String repliedAt;

        public ReviewReplyDto() {
        }

        public ReviewReplyDto(String content, String repliedAt) {
            this.content = content;
            this.repliedAt = repliedAt;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRepliedAt() {
            return repliedAt;
        }

        public void setRepliedAt(String repliedAt) {
            this.repliedAt = repliedAt;
        }

        @Override
        public String toString() {
            return "ReviewReplyDto{" +
                    "content='" + content + '\'' +
                    ", repliedAt='" + repliedAt + '\'' +
                    '}';
        }
    }

    public static class Deserializer implements JsonDeserializer<ReviewDto> {
        @Override
        public ReviewDto deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ReviewDto review = new ReviewDto();
            JsonObject object = asObject(json);
            if (object == null) return review;

            review.id = firstString(object, "_id", "id");

            JsonElement orderDetailElement = firstElement(object, "order_detail_id", "orderDetailId");
            JsonObject orderDetail = asObject(orderDetailElement);
            review.orderDetailId = extractId(orderDetailElement);

            review.productId = firstString(object, "productId", "product_id");
            review.productSlug = firstString(object, "productSlug", "product_slug", "slug");
            review.productName = firstString(object, "productName", "product_name");
            review.productImageUrl = firstString(object, "productImageUrl", "product_image_url", "image_url");
            review.orderCode = firstString(object, "orderCode", "order_code");

            if (orderDetail != null) {
                if (isBlank(review.productName)) {
                    review.productName = firstString(orderDetail, "product_name", "productName");
                }
                JsonObject order = asObject(firstElement(orderDetail, "order_id", "orderId"));
                if (order != null && isBlank(review.orderCode)) {
                    review.orderCode = firstString(order, "order_code", "orderCode");
                }

                JsonObject variant = asObject(firstElement(orderDetail, "variant_id", "variantId"));
                if (variant != null) {
                    if (isBlank(review.productImageUrl)) {
                        review.productImageUrl = firstString(variant, "image", "image_url", "imageUrl");
                    }

                    JsonElement productElement = firstElement(variant, "product_id", "productId");
                    JsonObject product = asObject(productElement);
                    if (isBlank(review.productId)) {
                        review.productId = extractId(productElement);
                    }
                    if (product != null) {
                        if (isBlank(review.productSlug)) {
                            review.productSlug = firstString(product, "slug");
                        }
                        if (isBlank(review.productImageUrl)) {
                            review.productImageUrl = firstString(product, "thumbnail_url", "thumbnailUrl", "thumbnail");
                        }
                    }
                }
            }

            JsonElement customerElement = firstElement(object, "customerId", "customer_id");
            JsonObject customer = asObject(customerElement);
            review.customerId = extractId(customerElement);
            review.customerName = firstString(object, "customerName", "customer_name");
            if (customer != null && isBlank(review.customerName)) {
                review.customerName = firstString(customer, "full_name", "fullName", "name");
            }

            review.rating = firstInteger(object, "rating");
            review.content = firstString(object, "content");
            review.images = stringList(firstElement(object, "images"));
            review.videos = stringList(firstElement(object, "videos"));
            review.status = firstString(object, "status");
            review.createdAt = firstString(object, "createdAt", "created_at");

            JsonObject reply = asObject(firstElement(object, "reply"));
            if (reply != null) {
                String replyContent = firstString(reply, "content");
                if (!isBlank(replyContent)) {
                    review.reply = new ReviewReplyDto(replyContent, firstString(reply, "repliedAt", "replied_at"));
                }
            }

            return review;
        }

        private static JsonElement firstElement(JsonObject object, String... names) {
            if (object == null || names == null) return null;
            for (String name : names) {
                if (object.has(name) && !object.get(name).isJsonNull()) {
                    return object.get(name);
                }
            }
            return null;
        }

        private static String firstString(JsonObject object, String... names) {
            JsonElement element = firstElement(object, names);
            return elementToString(element);
        }

        private static Integer firstInteger(JsonObject object, String... names) {
            JsonElement element = firstElement(object, names);
            if (element == null || element.isJsonNull()) return null;
            try {
                return element.getAsInt();
            } catch (Exception ignored) {
                return null;
            }
        }

        private static List<String> stringList(JsonElement element) {
            List<String> values = new ArrayList<>();
            JsonArray array = element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
            if (array == null) return values;
            for (JsonElement item : array) {
                String value = elementToString(item);
                if (!isBlank(value)) values.add(value);
            }
            return values;
        }

        private static JsonObject asObject(JsonElement element) {
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        }

        private static String elementToString(JsonElement element) {
            String value = primitiveString(element);
            return !isBlank(value) ? value : extractId(element);
        }

        private static String extractId(JsonElement element) {
            String direct = primitiveString(element);
            if (!isBlank(direct)) return direct;

            JsonObject object = asObject(element);
            if (object == null) return null;

            String id = primitiveString(firstElement(object, "_id", "id", "$oid"));
            if (!isBlank(id)) return id;

            JsonObject nestedId = asObject(firstElement(object, "_id", "id"));
            if (nestedId != null) {
                id = primitiveString(firstElement(nestedId, "$oid"));
            }
            return isBlank(id) ? null : id;
        }

        private static String primitiveString(JsonElement element) {
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return null;
            try {
                String value = element.getAsString();
                return isBlank(value) ? null : value.trim();
            } catch (Exception ignored) {
                return null;
            }
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
