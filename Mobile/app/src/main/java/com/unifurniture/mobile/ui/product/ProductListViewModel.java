package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductListViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<ApiListResponse<ProductDto>> products = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();

    private final List<ProductDto> allProducts = new ArrayList<>();
    private boolean hasMore = true;

    private static final List<ProductDto> sharedProductsCache = new ArrayList<>();
    private static int sharedTotalCache = 0;
    private static int sharedScrollPosition = RecyclerView.NO_POSITION;
    private static int sharedScrollOffset = 0;

    private int currentPage = 1;
    private static final int PAGE_SIZE = 50;
    private String currentSearch = null;
    private String currentCategory = null;
    private String currentCollection = null;
    private String currentSortBy = "createdAt";
    private String currentOrder = "desc";
    private Double currentMinPrice = null;
    private Double currentMaxPrice = null;
    private int currentMinRating = 0;
    private int savedScrollPosition = RecyclerView.NO_POSITION;
    private int savedScrollOffset = 0;
    private int cachedTotal = 0;

    // Track observers to prevent memory leaks
    private LiveData<ApiListResponse<ProductDto>> fetchLiveData;
    private Observer<ApiListResponse<ProductDto>> fetchObserver;
    private LiveData<List<CategoryDto>> categoriesLiveData;
    private final Observer<List<CategoryDto>> categoriesObserver = categories::setValue;

    public ProductListViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        loadCategories();
        restoreSharedCache();
    }

    private void restoreSharedCache() {
        if (sharedProductsCache.isEmpty()) return;
        allProducts.clear();
        allProducts.addAll(sharedProductsCache);
        cachedTotal = sharedTotalCache;
        savedScrollPosition = sharedScrollPosition;
        savedScrollOffset = sharedScrollOffset;

        ApiListResponse<ProductDto> restored = new ApiListResponse<>();
        restored.items = new ArrayList<>(allProducts);
        restored.total = cachedTotal;
        products.setValue(restored);
    }

    public void loadProducts() {
        loadProducts(true, true);
    }

    public void loadProductsIfNeeded() {
        if (hasLoadedProducts()) return;
        loadProducts(false, allProducts.isEmpty());
    }

    public void refreshProducts() {
        loadProducts(true, true);
    }

    private void loadProducts(boolean resetScroll, boolean clearExistingProducts) {
        currentPage = 1;
        if (clearExistingProducts) {
            allProducts.clear();
        }
        hasMore = true;
        if (resetScroll) {
            clearSavedScrollState();
        }
        
        // Bật loading TRƯỚC khi reset dữ liệu để UI không hiện "Trống" nhầm
        loading.setValue(true);
        
        if (clearExistingProducts) {
            ApiListResponse<ProductDto> emptyResponse = new ApiListResponse<>();
            emptyResponse.items = new ArrayList<>();
            emptyResponse.total = 0;
            products.setValue(emptyResponse);
        }

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

        // Remove previous fetch observer before creating a new one
        if (fetchLiveData != null && fetchObserver != null) {
            fetchLiveData.removeObserver(fetchObserver);
        }

        fetchObserver = response -> {
            if (isLoadMore) loadingMore.setValue(false);
            else loading.setValue(false);

            if (response != null && response.items != null) {
                List<ProductDto> pageItems = response.items;

                hasMore = response.page < response.totalPages;
                allProducts.addAll(pageItems);

                ApiListResponse<ProductDto> result = new ApiListResponse<>();
                result.items = new ArrayList<>(allProducts);
                result.total = response.total;
                cachedTotal = response.total;
                sharedProductsCache.clear();
                sharedProductsCache.addAll(allProducts);
                sharedTotalCache = cachedTotal;
                products.setValue(result);
            } else {
                hasMore = false;
                if (!isLoadMore) products.setValue(null);
            }
        };

        fetchLiveData = repository.getProducts(currentPage, PAGE_SIZE, currentSearch,
                currentCategory, currentCollection, currentSortBy, currentOrder,
                currentMinPrice, currentMaxPrice,
                currentMinRating > 0 ? currentMinRating : null);
        fetchLiveData.observeForever(fetchObserver);
    }

    public void search(String query) {
        currentSearch = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
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

    public void applyRequest(String search, String categoryId, String collectionId) {
        currentSearch = normalizeBlank(search);
        currentCategory = normalizeBlank(categoryId);
        currentCollection = normalizeBlank(collectionId);
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
        categoriesLiveData = repository.getCategories();
        categoriesLiveData.observeForever(categoriesObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (fetchLiveData != null && fetchObserver != null)
            fetchLiveData.removeObserver(fetchObserver);
        if (categoriesLiveData != null)
            categoriesLiveData.removeObserver(categoriesObserver);
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
    public boolean hasMorePages() { return hasMore; }
    public int getCachedTotal() { return cachedTotal; }

    public List<ProductDto> getCachedProducts() {
        return new ArrayList<>(allProducts);
    }

    public boolean matchesCurrentRequest(String search, String categoryId, String collectionId) {
        return Objects.equals(normalizeBlank(search), currentSearch)
                && Objects.equals(normalizeBlank(categoryId), currentCategory)
                && Objects.equals(normalizeBlank(collectionId), currentCollection);
    }

    private static String normalizeBlank(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void saveScrollState(int position, int offset) {
        savedScrollPosition = position;
        savedScrollOffset = offset;
        sharedScrollPosition = position;
        sharedScrollOffset = offset;
    }

    public boolean hasSavedScrollState() {
        return savedScrollPosition != RecyclerView.NO_POSITION;
    }

    public int getSavedScrollPosition() { return savedScrollPosition; }
    public int getSavedScrollOffset() { return savedScrollOffset; }

    public String getCurrentScrollKey() {
        return normalizeForKey(currentSearch) + "|"
                + normalizeForKey(currentCategory) + "|"
                + normalizeForKey(currentCollection) + "|"
                + normalizeForKey(currentSortBy) + "|"
                + normalizeForKey(currentOrder) + "|"
                + normalizeForKey(currentMinPrice) + "|"
                + normalizeForKey(currentMaxPrice) + "|"
                + currentMinRating;
    }

    private static String normalizeForKey(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public void clearSavedScrollState() {
        savedScrollPosition = RecyclerView.NO_POSITION;
        savedScrollOffset = 0;
        sharedScrollPosition = RecyclerView.NO_POSITION;
        sharedScrollOffset = 0;
    }

    public boolean hasLoadedProducts() {
        return products.getValue() != null;
    }
}
