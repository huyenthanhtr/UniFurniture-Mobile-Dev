package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
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
    private final MutableLiveData<Boolean> addToCartResult = new MutableLiveData<>();
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

    /**
     * Add to cart:
     * 1. Get active cart (to obtain cart_id)
     * 2. Upsert item with cart_id, variant_id, quantity, unit_price
     */
    public void addToCart(String variantId) {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        ProductDto p = product.getValue();
        if (customerId == null || p == null) {
            error.setValue("Vui lòng đăng nhập để thêm vào giỏ hàng");
            return;
        }

        // Get the selected variant's price
        double unitPrice = p.minPrice != null ? p.minPrice : 0;
        ApiListResponse<ProductVariantDto> variantList = variants.getValue();
        if (variantList != null && variantList.items != null && variantId != null) {
            for (ProductVariantDto v : variantList.items) {
                if (variantId.equals(v.id) && v.price != null) {
                    unitPrice = v.price;
                    break;
                }
            }
        }

        final double finalPrice = unitPrice;

        // First ensure we have a cart
        String cachedCartId = session.getCartId();
        if (cachedCartId != null) {
            // Use cached cart ID
            cartRepo.upsertCartItem(cachedCartId, variantId, 1, finalPrice)
                    .observeForever(item -> addToCartResult.setValue(item != null));
        } else {
            // Load cart to get cart_id
            cartRepo.getActiveCart(customerId).observeForever(cart -> {
                if (cart != null && cart.getCartId() != null) {
                    session.saveCartId(cart.getCartId());
                    cartRepo.upsertCartItem(cart.getCartId(), variantId, 1, finalPrice)
                            .observeForever(item -> addToCartResult.setValue(item != null));
                } else {
                    error.setValue("Không thể tải giỏ hàng");
                }
            });
        }
    }

    public LiveData<ProductDto> getProduct() { return product; }
    public LiveData<ApiListResponse<ProductImageDto>> getImages() { return images; }
    public LiveData<ApiListResponse<ProductVariantDto>> getVariants() { return variants; }
    public LiveData<ReviewSummaryDto> getReviews() { return reviews; }
    public LiveData<Boolean> getAddToCartResult() { return addToCartResult; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
