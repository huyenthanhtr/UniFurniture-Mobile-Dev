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

    public LiveData<ApiListResponse<CategoryDto>> getCategories() {
        MutableLiveData<ApiListResponse<CategoryDto>> result = new MutableLiveData<>();
        apiService.getCategories()
                .enqueue(new Callback<List<CategoryDto>>() {
                    @Override
                    public void onResponse(Call<List<CategoryDto>> call,
                                           Response<List<CategoryDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiListResponse<CategoryDto> wrapped = new ApiListResponse<>();
                            wrapped.items = response.body();
                            wrapped.total = response.body().size();
                            wrapped.limit = response.body().size();
                            wrapped.page = 1;
                            wrapped.totalPages = 1;
                            result.setValue(wrapped);
                        } else {
                            result.setValue(null);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<CategoryDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }

    public LiveData<ApiListResponse<CollectionDto>> getCollections() {
        MutableLiveData<ApiListResponse<CollectionDto>> result = new MutableLiveData<>();
        apiService.getCollections()
                .enqueue(new Callback<List<CollectionDto>>() {
                    @Override
                    public void onResponse(Call<List<CollectionDto>> call,
                                           Response<List<CollectionDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiListResponse<CollectionDto> wrapped = new ApiListResponse<>();
                            wrapped.items = response.body();
                            wrapped.total = response.body().size();
                            wrapped.limit = response.body().size();
                            wrapped.page = 1;
                            wrapped.totalPages = 1;
                            result.setValue(wrapped);
                        } else {
                            result.setValue(null);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<CollectionDto>> call, Throwable t) {
                        result.setValue(null);
                    }
                });
        return result;
    }
}
