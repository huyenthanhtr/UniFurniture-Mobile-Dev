package com.unifurniture.mobile.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.ProductRepository;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<ApiListResponse<ProductDto>> featuredProducts = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<CollectionDto>> collections = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ProductDto>> searchSuggestions = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        loadData();
    }

    public void loadData() {
        loading.setValue(true);

        // Load featured products (latest 10)
        repository.getProducts(1, 10, null, null, null, "createdAt", "desc", null, null)
                .observeForever(response -> {
                    featuredProducts.setValue(response);
                    loading.setValue(false);
                });

        repository.getCategories().observeForever(categories::setValue);
        repository.getCollections().observeForever(collections::setValue);
    }

    public void searchForSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchSuggestions.setValue(null);
            return;
        }
        repository.getProducts(1, 8, query.trim(), null, null, "createdAt", "desc", null, null)
                .observeForever(response -> {
                    if (response != null && response.items != null) {
                        searchSuggestions.setValue(response.items);
                    } else {
                        searchSuggestions.setValue(null);
                    }
                });
    }

    public LiveData<ApiListResponse<ProductDto>> getFeaturedProducts() { return featuredProducts; }
    public LiveData<List<CategoryDto>> getCategories() { return categories; }
    public LiveData<List<CollectionDto>> getCollections() { return collections; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<List<ProductDto>> getSearchSuggestions() { return searchSuggestions; }
}
