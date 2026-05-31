package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.ProductRepository;
import java.util.List;

public class ProductListViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<ApiListResponse<ProductDto>> products = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    private String currentSearch = null;
    private String currentCategory = null;
    private String currentCollection = null;
    private String currentSortBy = "createdAt";
    private String currentOrder = "desc";

    public ProductListViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        loadProducts();
        loadCategories();
    }

    public void loadProducts() {
        loading.setValue(true);
        repository.getProducts(currentPage, PAGE_SIZE, currentSearch,
                        currentCategory, currentCollection, currentSortBy, currentOrder)
                .observeForever(response -> {
                    products.setValue(response);
                    loading.setValue(false);
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

    public void clearFilters() {
        currentSearch = null;
        currentCategory = null;
        currentCollection = null;
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
    public LiveData<List<CategoryDto>> getCategories() { return categories; }
}
