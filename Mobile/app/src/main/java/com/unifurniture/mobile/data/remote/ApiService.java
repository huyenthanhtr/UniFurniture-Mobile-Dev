package com.unifurniture.mobile.data.remote;

import com.unifurniture.mobile.data.model.*;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    // Server expects: { phone, password_hash, full_name }
    @POST("auth/register")
    Call<AuthResponse> register(@Body Map<String, String> body);

    // Server expects: { phone, otp }  →  returns { message, profile }
    @POST("auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body Map<String, String> body);

    // Server expects: { emailOrPhone, password }  →  returns { message, profile }
    @POST("auth/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    // Server expects: { phone }
    @POST("auth/forgot-password")
    Call<AuthResponse> forgotPassword(@Body Map<String, String> body);

    // Server expects: { phone, otp, newPassword }
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
    Call<List<CategoryDto>> getCategories();

    @GET("collections")
    Call<List<CollectionDto>> getCollections();

    // ── Cart ──────────────────────────────────────────────────────────────────
    // Server returns: { cart: {...}, items: [...] }
    @GET("cart/active")
    Call<CartDto> getActiveCart(@Query("customer_id") String customerId);

    // Server expects: { cart_id, variant_id, quantity, unit_price }
    // Server returns: single populated CartItemDto
    @POST("cart/items/upsert")
    Call<CartItemDto> upsertCartItem(@Body Map<String, Object> body);

    // Server expects: { quantity }
    // Server returns: { merged, item }
    @PATCH("cart/items/{id}")
    Call<Map<String, Object>> updateCartItem(
            @Path("id") String cartItemId,
            @Body Map<String, Integer> body
    );

    // Server returns: { success, deleted }
    @DELETE("cart/items/{id}")
    Call<Map<String, Object>> deleteCartItem(@Path("id") String cartItemId);

    // ── Orders ────────────────────────────────────────────────────────────────
    // Server filters by account_id (profile._id)
    @GET("orders")
    Call<ApiListResponse<OrderDto>> getOrders(@Query("customer_id") String accountId);

    @GET("orders/{id}")
    Call<OrderDto> getOrderById(@Path("id") String orderId);

    // Server expects: { account_id, shipping_name, shipping_phone, shipping_address, payment_method, items: [...] }
    @POST("orders")
    Call<CheckoutResponse> createOrder(@Body CheckoutRequest request);

    // Server expects: { phone, reason }
    @POST("orders/{id}/cancel-request")
    Call<OrderDto> requestCancelOrder(
            @Path("id") String orderId,
            @Body Map<String, String> body
    );

    // ── Order Tracking ────────────────────────────────────────────────────────
    @GET("orders")
    Call<ApiListResponse<OrderDto>> trackOrderByCode(
            @Query("tracking_code") String trackingCode
    );

    // ── Reviews ───────────────────────────────────────────────────────────────
    @GET("reviews/product/{productId}")
    Call<ReviewSummaryDto> getProductReviews(@Path("productId") String productId);

    @GET("reviews/order/{orderId}/status")
    Call<Map<String, Object>> getOrderReviewStatus(@Path("orderId") String orderId);

    // ── Wishlist ──────────────────────────────────────────────────────────────
    // Server route: GET /wishlist/profiles/:profileId
    @GET("wishlist/profiles/{profileId}")
    Call<WishlistListResponse> getWishlist(@Path("profileId") String profileId);

    // Server route: POST /wishlist/profiles/:profileId/items
    @POST("wishlist/profiles/{profileId}/items")
    Call<WishlistUpsertResponse> addToWishlist(
            @Path("profileId") String profileId,
            @Body Map<String, Object> body
    );

    // Server route: DELETE /wishlist/profiles/:profileId/items/:productId
    @DELETE("wishlist/profiles/{profileId}/items/{productId}")
    Call<Map<String, Object>> removeFromWishlist(
            @Path("profileId") String profileId,
            @Path("productId") String productId
    );

    // ── Profile ───────────────────────────────────────────────────────────────
    @GET("profiles/{id}")
    Call<ProfileDto> getProfileById(@Path("id") String profileId);

    @PATCH("profiles/{id}")
    Call<ProfileDto> updateProfile(
            @Path("id") String profileId,
            @Body Map<String, String> body
    );

    @POST("profiles/{id}/change-password")
    Call<Map<String, String>> changePassword(
            @Path("id") String profileId,
            @Body Map<String, String> body
    );

    // ── Customers ─────────────────────────────────────────────────────────────
    @GET("customers/{id}")
    Call<CustomerDto> getCustomer(@Path("id") String customerId);

    // ── Loyalty ───────────────────────────────────────────────────────────────
    @GET("loyalty/profiles/{profileId}")
    Call<Map<String, Object>> getProfileLoyalty(@Path("profileId") String profileId);

    @GET("loyalty/estimate")
    Call<Map<String, Object>> estimateLoyaltyPoints(@Query("orderValue") double orderValue);

    // ── Coupons ───────────────────────────────────────────────────────────────
    @GET("coupons")
    Call<List<Map<String, Object>>> getCoupons();
}
