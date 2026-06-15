package com.unifurniture.mobile.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.remote.ApiService;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductRepository {

    private final ApiService apiService;

    public ProductRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<ApiListResponse<ProductDto>> getProducts(
            int page, int limit, String search, String categories, String collection,
            String sortBy, String order, Double minPrice, Double maxPrice) {
        MutableLiveData<ApiListResponse<ProductDto>> result = new MutableLiveData<>();
        apiService.getProducts(page, limit, "active", sortBy, order, search, categories, collection, minPrice, maxPrice, null)
                .enqueue(new Callback<ApiListResponse<ProductDto>>() {
                    @Override
                    public void onResponse(Call<ApiListResponse<ProductDto>> call,
                                           Response<ApiListResponse<ProductDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            result.setValue(null);
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiListResponse<ProductDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    public LiveData<ProductDto> getProductDetail(String slug) {
        MutableLiveData<ProductDto> result = new MutableLiveData<>();
        apiService.getProductBySlug(slug).enqueue(new Callback<ProductDto>() {
            @Override
            public void onResponse(Call<ProductDto> call, Response<ProductDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<ProductDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<ApiListResponse<ProductImageDto>> getProductImages(String productId) {
        MutableLiveData<ApiListResponse<ProductImageDto>> result = new MutableLiveData<>();
        apiService.getProductImages(productId, 200, "sort_order")
                .enqueue(new Callback<ApiListResponse<ProductImageDto>>() {
                    @Override
                    public void onResponse(Call<ApiListResponse<ProductImageDto>> call,
                                           Response<ApiListResponse<ProductImageDto>> response) {
                        result.setValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<ApiListResponse<ProductImageDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    public LiveData<ApiListResponse<ProductVariantDto>> getProductVariants(String productId) {
        MutableLiveData<ApiListResponse<ProductVariantDto>> result = new MutableLiveData<>();
        apiService.getProductVariants(productId, "active", 200)
                .enqueue(new Callback<ApiListResponse<ProductVariantDto>>() {
                    @Override
                    public void onResponse(Call<ApiListResponse<ProductVariantDto>> call,
                                           Response<ApiListResponse<ProductVariantDto>> response) {
                        result.setValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<ApiListResponse<ProductVariantDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    public LiveData<ReviewSummaryDto> getProductReviews(String productId) {
        MutableLiveData<ReviewSummaryDto> result = new MutableLiveData<>();
        apiService.getProductReviews(productId).enqueue(new Callback<ReviewSummaryDto>() {
            @Override
            public void onResponse(Call<ReviewSummaryDto> call, Response<ReviewSummaryDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<ReviewSummaryDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<List<ProductDto>> getProductRecommendations(String slug, String userId) {
        MutableLiveData<List<ProductDto>> result = new MutableLiveData<>();
        apiService.getProductRecommendations(slug, userId)
                .enqueue(new Callback<java.util.Map<String, List<ProductDto>>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.Map<String, List<ProductDto>>> call,
                                           retrofit2.Response<java.util.Map<String, List<ProductDto>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ProductDto> items = response.body().get("items");
                            result.setValue(items != null ? items : new java.util.ArrayList<>());
                        } else {
                            result.setValue(null);
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<java.util.Map<String, List<ProductDto>>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    public LiveData<List<CategoryDto>> getCategories() {
        MutableLiveData<List<CategoryDto>> result = new MutableLiveData<>();
        apiService.getCategories(1, 100, "active")
                .enqueue(new Callback<List<CategoryDto>>() {
                    @Override
                    public void onResponse(Call<List<CategoryDto>> call,
                                           Response<List<CategoryDto>> response) {
                        result.setValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<List<CategoryDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    public LiveData<List<CollectionDto>> getCollections() {
        MutableLiveData<List<CollectionDto>> result = new MutableLiveData<>();
        apiService.getCollections(1, 100, "active")
                .enqueue(new Callback<List<CollectionDto>>() {
                    @Override
                    public void onResponse(Call<List<CollectionDto>> call,
                                           Response<List<CollectionDto>> response) {
                        result.setValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<List<CollectionDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    // ── Wishlist ──────────────────────────────────────────────────────────────

    public LiveData<List<WishlistItemDto>> getWishlist(String customerId) {
        MutableLiveData<List<WishlistItemDto>> result = new MutableLiveData<>();
        apiService.getWishlist(customerId).enqueue(new Callback<ApiListResponse<WishlistItemDto>>() {
            @Override
            public void onResponse(Call<ApiListResponse<WishlistItemDto>> call, Response<ApiListResponse<WishlistItemDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body().items);
                } else {
                    result.setValue(null);
                }
            }
            @Override
            public void onFailure(Call<ApiListResponse<WishlistItemDto>> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<WishlistItemDto> addToWishlist(String customerId, String productId) {
        MutableLiveData<WishlistItemDto> result = new MutableLiveData<>();
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("customer_id", customerId);
        body.put("product_id", productId);
        apiService.addToWishlist(body).enqueue(new Callback<WishlistItemDto>() {
            @Override
            public void onResponse(Call<WishlistItemDto> call, Response<WishlistItemDto> response) {
                result.setValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<WishlistItemDto> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public LiveData<Boolean> removeFromWishlist(String wishlistItemId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        apiService.removeFromWishlist(wishlistItemId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }
}
