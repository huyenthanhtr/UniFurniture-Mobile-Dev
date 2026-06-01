package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;

public class ProductListViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<ApiListResponse<ProductDto>> products = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();

    private final List<ProductDto> allProducts = new ArrayList<>();
    private boolean hasMore = true;

    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    private String currentSearch = null;
    private String currentCategory = null;
    private String currentCollection = null;
    private String currentSortBy = "createdAt";
    private String currentOrder = "desc";
    private Double currentMinPrice = null;
    private Double currentMaxPrice = null;
    private int currentMinRating = 0;

    public ProductListViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        loadCategories();
    }

    public void loadProducts() {
        currentPage = 1;
        allProducts.clear();
        hasMore = true;
        fetchCurrentPage(false);
    }

    public void loadNextPage() {
        if (!canLoadMore()) return;
        currentPage++;
        fetchCurrentPage(true);
    }

    public boolean canLoadMore() {
        return hasMore
                && !Boolean.TRUE.equals(loading.getValue())
                && !Boolean.TRUE.equals(loadingMore.getValue());
    }

    private void fetchCurrentPage(boolean isLoadMore) {
        if (isLoadMore) loadingMore.setValue(true);
        else loading.setValue(true);

        repository.getProducts(currentPage, PAGE_SIZE, currentSearch,
                        currentCategory, currentCollection, currentSortBy, currentOrder,
                        currentMinPrice, currentMaxPrice)
                .observeForever(response -> {
                    if (isLoadMore) loadingMore.setValue(false);
                    else loading.setValue(false);

                    if (response != null && response.items != null) {
                        List<ProductDto> pageItems = response.items;

                        if (currentMinRating > 0) {
                            List<ProductDto> filtered = new ArrayList<>();
                            for (ProductDto p : pageItems) {
                                if (p.averageRating == null || p.averageRating >= currentMinRating) {
                                    filtered.add(p);
                                }
                            }
                            pageItems = filtered;
                        }

                        hasMore = response.page < response.totalPages;
                        allProducts.addAll(pageItems);

                        ApiListResponse<ProductDto> result = new ApiListResponse<>();
                        result.items = new ArrayList<>(allProducts);
                        result.total = response.total;
                        products.setValue(result);
                    } else {
                        hasMore = false;
                        if (!isLoadMore) products.setValue(null);
                    }
                });
    }

    public void search(String query) {
        currentSearch = query;
        currentPage = 1;
        loadProducts();
    }

    public void filterByCategory(String categoryId) {
        currentCategory = categoryId;
        currentPage = 1;
        loadProducts();
    }

    public void filterByCollection(String collectionId) {
        currentCollection = collectionId;
        currentPage = 1;
        loadProducts();
    }

    public void sortBy(String sortBy, String order) {
        currentSortBy = sortBy;
        currentOrder = order;
        currentPage = 1;
        loadProducts();
    }

    public void applyFilters(String categoryId, Double minPrice, Double maxPrice, int minRating) {
        currentCategory = categoryId;
        currentMinPrice = minPrice;
        currentMaxPrice = maxPrice;
        currentMinRating = minRating;
        currentPage = 1;
        loadProducts();
    }

    public void clearFilters() {
        currentSearch = null;
        currentCategory = null;
        currentCollection = null;
        currentMinPrice = null;
        currentMaxPrice = null;
        currentMinRating = 0;
        currentSortBy = "createdAt";
        currentOrder = "desc";
        currentPage = 1;
        loadProducts();
    }

    private void loadCategories() {
        repository.getCategories().observeForever(categories::setValue);
    }

    public LiveData<ApiListResponse<ProductDto>> getProducts() { return products; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<Boolean> isLoadingMore() { return loadingMore; }
    public LiveData<List<CategoryDto>> getCategories() { return categories; }

    public String getCurrentCategoryId() { return currentCategory; }
    public Double getCurrentMinPrice() { return currentMinPrice; }
    public Double getCurrentMaxPrice() { return currentMaxPrice; }
    public int getCurrentMinRating() { return currentMinRating; }
    public String getCurrentSortBy() { return currentSortBy; }
    public String getCurrentOrder() { return currentOrder; }
}
