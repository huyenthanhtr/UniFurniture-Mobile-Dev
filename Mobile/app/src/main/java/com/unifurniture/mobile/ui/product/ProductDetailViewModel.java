package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.CartRepository;
import com.unifurniture.mobile.data.repository.ProductRepository;
import com.unifurniture.mobile.util.SessionManager;

public class ProductDetailViewModel extends AndroidViewModel {

    private final ProductRepository productRepo;
    private final CartRepository cartRepo;
    private final MutableLiveData<ProductDto> product = new MutableLiveData<>();
    private final MutableLiveData<ApiListResponse<ProductImageDto>> images = new MutableLiveData<>();
    private final MutableLiveData<ApiListResponse<ProductVariantDto>> variants = new MutableLiveData<>();
    private final MutableLiveData<ReviewSummaryDto> reviews = new MutableLiveData<>();
    private final MutableLiveData<CartDto> addToCartResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProductDetailViewModel(@NonNull Application application) {
        super(application);
        productRepo = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        cartRepo = new CartRepository(UniFurnitureApp.getInstance().getApiService());
    }

    public void loadProduct(String slug) {
        loading.setValue(true);
        productRepo.getProductDetail(slug).observeForever(p -> {
            product.setValue(p);
            if (p != null) {
                loadImages(p.id);
                loadVariants(p.id);
                loadReviews(p.id);
            }
            loading.setValue(false);
        });
    }

    private void loadImages(String productId) {
        productRepo.getProductImages(productId).observeForever(images::setValue);
    }

    private void loadVariants(String productId) {
        productRepo.getProductVariants(productId).observeForever(variants::setValue);
    }

    private void loadReviews(String productId) {
        productRepo.getProductReviews(productId).observeForever(reviews::setValue);
    }

    public void addToCart(String variantId) {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        ProductDto p = product.getValue();
        if (customerId == null || p == null) {
            error.setValue("Vui lòng đăng nhập để thêm vào giỏ hàng");
            return;
        }
        cartRepo.addToCart(customerId, p.id, variantId, 1)
                .observeForever(addToCartResult::setValue);
    }

    public LiveData<ProductDto> getProduct() { return product; }
    public LiveData<ApiListResponse<ProductImageDto>> getImages() { return images; }
    public LiveData<ApiListResponse<ProductVariantDto>> getVariants() { return variants; }
    public LiveData<ReviewSummaryDto> getReviews() { return reviews; }
    public LiveData<CartDto> getAddToCartResult() { return addToCartResult; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
