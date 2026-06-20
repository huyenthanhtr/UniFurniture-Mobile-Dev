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
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<ApiListResponse<ProductDto>> featuredProducts = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<CollectionDto>> collections = new MutableLiveData<>();
    private final MutableLiveData<List<CouponDto>> coupons = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ProductDto>> searchSuggestions = new MutableLiveData<>();

    // Track observers so we can remove them in onCleared()
    private LiveData<ApiListResponse<ProductDto>> featuredLiveData;
    private LiveData<List<CategoryDto>> categoriesLiveData;
    private LiveData<List<CollectionDto>> collectionsLiveData;
    private LiveData<List<CouponDto>> couponsLiveData;
    private LiveData<ApiListResponse<ProductDto>> suggestionsLiveData;
    private Observer<ApiListResponse<ProductDto>> suggestionsObserver;

    private final Observer<ApiListResponse<ProductDto>> featuredObserver = r -> {
        featuredProducts.setValue(r);
        loading.setValue(false);
    };
    private final Observer<List<CategoryDto>> categoriesObserver = categories::setValue;
    private final Observer<List<CollectionDto>> collectionsObserver = collections::setValue;
    private final Observer<List<CouponDto>> couponsObserver = coupons::setValue;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        loadData();
    }

    public void loadData() {
        loading.setValue(true);

        // Remove old observers before re-subscribing
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);

        featuredLiveData = repository.getProducts(1, 10, null, null, null, "createdAt", "desc", null, null);
        featuredLiveData.observeForever(featuredObserver);

        categoriesLiveData = repository.getCategories();
        categoriesLiveData.observeForever(categoriesObserver);

        collectionsLiveData = repository.getCollections();
        collectionsLiveData.observeForever(collectionsObserver);

        couponsLiveData = repository.getCoupons();
        couponsLiveData.observeForever(couponsObserver);
    }

    public void searchForSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchSuggestions.setValue(null);
            return;
        }
        // Remove previous suggestion observer before creating a new one
        if (suggestionsLiveData != null && suggestionsObserver != null) {
            suggestionsLiveData.removeObserver(suggestionsObserver);
        }
        suggestionsObserver = response ->
                searchSuggestions.setValue(
                        (response != null && response.items != null) ? response.items : null);
        suggestionsLiveData = repository.getProducts(
                1, 8, query.trim(), null, null, "createdAt", "desc", null, null);
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
