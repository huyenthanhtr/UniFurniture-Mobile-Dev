package com.unifurniture.mobile.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.ProductRepository;
import com.unifurniture.mobile.util.FormatUtil;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<ApiListResponse<ProductDto>> featuredProducts = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<CollectionDto>> collections = new MutableLiveData<>();
    private final MutableLiveData<List<CouponDto>> coupons = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ProductDto>> searchSuggestions = new MutableLiveData<>();
    private final List<ProductDto> allProductsCache = new ArrayList<>();

    // Track observers so we can remove them in onCleared()
    private LiveData<ApiListResponse<ProductDto>> featuredLiveData;
    private LiveData<List<CategoryDto>> categoriesLiveData;
    private LiveData<List<CollectionDto>> collectionsLiveData;
    private LiveData<List<CouponDto>> couponsLiveData;
    private LiveData<ApiListResponse<ProductDto>> suggestionsLiveData;
    private Observer<ApiListResponse<ProductDto>> suggestionsObserver;

    private final Observer<ApiListResponse<ProductDto>> featuredObserver = r -> {
        featuredProducts.setValue(r);
        if (r != null && r.items != null) {
            // Update cache with featured products
            for (ProductDto p : r.items) {
                if (!allProductsCache.contains(p)) allProductsCache.add(p);
            }
        }
        loading.setValue(false);
    };
    private final Observer<List<CategoryDto>> categoriesObserver = categories::setValue;
    private final Observer<List<CollectionDto>> collectionsObserver = collections::setValue;
    private final Observer<List<CouponDto>> couponsObserver = coupons::setValue;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        if (featuredProducts.getValue() == null) {
            loadData();
        }
    }

    public void loadData() {
        if (featuredProducts.getValue() != null) {
            return;
        }
        loading.setValue(true);

        // Remove old observers before re-subscribing
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);

        featuredLiveData = repository.getProducts(1, 10, null, null, null, "createdAt", "desc", null, null, null);
        featuredLiveData.observeForever(featuredObserver);

        categoriesLiveData = repository.getCategories();
        categoriesLiveData.observeForever(categoriesObserver);

        collectionsLiveData = repository.getCollections();
        collectionsLiveData.observeForever(collectionsObserver);

        couponsLiveData = repository.getCoupons();
        couponsLiveData.observeForever(couponsObserver);
    }

    /** Pull-to-refresh: always fetch fresh data. */
    public void refreshData() {
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);
        featuredProducts.setValue(null);
        loadData();
    }

    private List<ProductDto> sortByRelevance(List<ProductDto> list, String query) {
        if (list == null || query == null || query.trim().isEmpty()) return list;
        String nQuery = FormatUtil.stripDiacritics(query.trim().toLowerCase());
        List<ProductDto> sorted = new ArrayList<>(list);
        sorted.sort((p1, p2) -> {
            String n1 = FormatUtil.stripDiacritics(p1.name != null ? p1.name.toLowerCase() : "");
            String n2 = FormatUtil.stripDiacritics(p2.name != null ? p2.name.toLowerCase() : "");

            boolean start1 = n1.startsWith(nQuery);
            boolean start2 = n2.startsWith(nQuery);
            if (start1 && !start2) return -1;
            if (!start1 && start2) return 1;

            boolean wordStart1 = n1.contains(" " + nQuery);
            boolean wordStart2 = n2.contains(" " + nQuery);
            if (wordStart1 && !wordStart2) return -1;
            if (!wordStart1 && wordStart2) return 1;

            return n1.compareTo(n2);
        });
        return sorted;
    }

    public void searchForSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchSuggestions.setValue(null);
            return;
        }

        String q = query.trim();
        // Instant local filter for snappy UI
        List<ProductDto> localMatches = new ArrayList<>();
        String normalizedQuery = FormatUtil.stripDiacritics(q.toLowerCase());
        for (ProductDto p : allProductsCache) {
            if (p.name != null) {
                String normalizedName = FormatUtil.stripDiacritics(p.name.toLowerCase());
                if (normalizedName.contains(normalizedQuery)) {
                    localMatches.add(p);
                }
            }
        }

        if (!localMatches.isEmpty()) {
            searchSuggestions.setValue(sortByRelevance(localMatches, q));
        }

        // Remove previous suggestion observer before creating a new one
        if (suggestionsLiveData != null && suggestionsObserver != null) {
            suggestionsLiveData.removeObserver(suggestionsObserver);
        }
        suggestionsObserver = response -> {
            if (response != null && response.items != null) {
                // Update cache with results from server
                for (ProductDto p : response.items) {
                    boolean exists = false;
                    for (ProductDto cached : allProductsCache) {
                        if (cached.id.equals(p.id)) { exists = true; break; }
                    }
                    if (!exists) allProductsCache.add(p);
                }
                // Always update suggestions if server returns something, sorted by relevance
                if (!response.items.isEmpty()) {
                    searchSuggestions.setValue(sortByRelevance(response.items, q));
                } else if (localMatches.isEmpty()) {
                    searchSuggestions.setValue(new ArrayList<>()); // Clear if nothing found
                }
            }
        };
        suggestionsLiveData = repository.getProducts(
                1, 8, q, null, null, "createdAt", "desc", null, null, null);
        suggestionsLiveData.observeForever(suggestionsObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);
        if (suggestionsLiveData != null && suggestionsObserver != null)
            suggestionsLiveData.removeObserver(suggestionsObserver);
    }

    public LiveData<ApiListResponse<ProductDto>> getFeaturedProducts() { return featuredProducts; }
    public LiveData<List<CategoryDto>> getCategories() { return categories; }
    public LiveData<List<CollectionDto>> getCollections() { return collections; }
    public LiveData<List<CouponDto>> getCoupons() { return coupons; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<List<ProductDto>> getSearchSuggestions() { return searchSuggestions; }
}
