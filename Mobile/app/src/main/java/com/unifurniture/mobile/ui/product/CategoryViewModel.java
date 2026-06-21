package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.data.repository.ProductRepository;
import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
    }

    public void loadCategoriesIfNeeded() {
        List<CategoryDto> cached = categories.getValue();
        if (cached != null && !cached.isEmpty()) {
            return;
        }
        loading.setValue(true);
        repository.getCategories().observeForever(list -> {
            categories.setValue(list);
            loading.setValue(false);
        });
    }

    public boolean hasCategories() {
        List<CategoryDto> cached = categories.getValue();
        return cached != null && !cached.isEmpty();
    }

    public LiveData<List<CategoryDto>> getCategories() { return categories; }
    public LiveData<Boolean> isLoading() { return loading; }
}
