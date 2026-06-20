package com.unifurniture.mobile.data.remote;

import com.unifurniture.mobile.data.model.*;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/register")
    Call<AuthResponse> register(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body Map<String, String> body);

    @POST("auth/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    @POST("auth/forgot-password")
    Call<AuthResponse> forgotPassword(@Body Map<String, String> body);

    @POST("auth/reset-password")
    Call<AuthResponse> resetPassword(@Body Map<String, String> body);

    // ── Products ──────────────────────────────────────────────────────────────
    @GET("products")
    Call<ApiListResponse<ProductDto>> getProducts(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status,
            @Query("sortBy") String sortBy,
            @Query("order") String order,
            @Query("q") String search,
            @Query("category") String categories,
            @Query("collection") String collection,
            @Query("minPrice") Double minPrice,
            @Query("maxPrice") Double maxPrice,
            @Query("fields") String fields
    );

    @GET("products/{slug}")
    Call<ProductDto> getProductBySlug(@Path("slug") String slug);

    @GET("products/{slug}/recommendations")
    Call<Map<String, List<ProductDto>>> getProductRecommendations(
            @Path("slug") String slug,
            @Query("user_id") String userId
    );

    @GET("product-images")
    Call<ApiListResponse<ProductImageDto>> getProductImages(
            @Query("product_id") String productId,
            @Query("limit") int limit,
            @Query("sort") String sort
    );

    @GET("product-variants")
    Call<ApiListResponse<ProductVariantDto>> getProductVariants(
            @Query("product_id") String productId,
            @Query("variant_status") String variantStatus,
            @Query("limit") int limit
    );

    // ── Categories & Collections ──────────────────────────────────────────────
    @GET("categories")
    Call<List<CategoryDto>> getCategories(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status
    );

    @GET("collections")
    Call<List<CollectionDto>> getCollections(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status
    );

    // ── Cart ──────────────────────────────────────────────────────────────────
    @GET("cart/active")
    Call<CartDto> getActiveCart(
            @Query("customer_id") String customerId,
            @Query("cart_id") String cartId
    );

    @POST("cart/items/upsert")
    Call<CartDto> upsertCartItem(@Body Map<String, Object> body);

    @PATCH("cart/items/{id}")
    Call<CartDto> updateCartItem(
            @Path("id") String cartItemId,
            @Body Map<String, Object> body
    );

    @DELETE("cart/items/{id}")
    Call<CartDto> deleteCartItem(@Path("id") String cartItemId);

    // ── Orders ────────────────────────────────────────────────────────────────
    @GET("orders")
    Call<ApiListResponse<OrderDto>> getOrders(@Query("customer_id") String customerId);

    @GET("orders/{id}")
    Call<OrderDetailResponse> getOrderById(@Path("id") String orderId);

    @POST("orders")
    Call<CheckoutResponse> createOrder(@Body CheckoutRequest request);

    @POST("orders/{id}/cancel-request")
    Call<OrderDto> requestCancelOrder(
            @Path("id") String orderId,
            @Body Map<String, String> body
    );

    @POST("orders/{id}/demo-transfer-timeout")
    Call<Map<String, Object>> completeDemoTransfer(@Path("id") String orderId);

    // ── Order Tracking ────────────────────────────────────────────────────────
    @GET("orders")
    Call<ApiListResponse<OrderDto>> trackOrderByCode(
            @Query("tracking_code") String trackingCode
    );

    // ── Reviews ───────────────────────────────────────────────────────────────
    @GET("reviews")
    Call<List<ReviewDto>> getReviews(@Query("customer_id") String customerId);

    @GET("reviews/product/{productId}")
    Call<ReviewSummaryDto> getProductReviews(@Path("productId") String productId);

    // ── Wishlist ──────────────────────────────────────────────────────────────
    @GET("wishlist")
    Call<ApiListResponse<WishlistItemDto>> getWishlist(@Query("customer_id") String customerId);

    @POST("wishlist")
    Call<WishlistItemDto> addToWishlist(@Body Map<String, String> body);

    @DELETE("wishlist/{id}")
    Call<Void> removeFromWishlist(@Path("id") String wishlistItemId);

    // ── Profile ───────────────────────────────────────────────────────────────
    @GET("profiles")
    Call<ApiListResponse<ProfileDto>> getProfile(
            @Query("customer_id") String customerId
    );

    @PATCH("profiles/{id}")
    Call<ProfileDto> updateProfile(
            @Path("id") String profileId,
            @Body Map<String, String> body
    );

    @POST("profiles/{id}/change-password")
    Call<Void> changePassword(
            @Path("id") String profileId,
            @Body Map<String, String> body
    );

    // ── Customers ─────────────────────────────────────────────────────────────
    @GET("customers/{id}")
    Call<CustomerDto> getCustomer(@Path("id") String customerId);

    // ── Addresses ─────────────────────────────────────────────────────────────
    @GET("customer-address")
    Call<ApiListResponse<CustomerAddressDto>> getCustomerAddresses(
            @Query("customer_id") String customerId,
            @Query("limit") int limit
    );

    @POST("customer-address")
    Call<CustomerAddressDto> createCustomerAddress(@Body Map<String, Object> body);

    @PATCH("customer-address/{id}")
    Call<CustomerAddressDto> updateCustomerAddress(
            @Path("id") String addressId,
            @Body Map<String, Object> body
    );

    @DELETE("customer-address/{id}")
    Call<Void> deleteCustomerAddress(@Path("id") String addressId);

    // ── Notifications ─────────────────────────────────────────────────────────
    @GET("notifications")
    Call<ApiListResponse<NotificationDto>> getNotifications(
            @Query("customer_id") String customerId,
            @Query("limit") int limit
    );

    // ── Vouchers ──────────────────────────────────────────────────────────────
    @GET("vouchers")
    Call<ApiListResponse<VoucherDto>> getVouchers(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status
    );
}
