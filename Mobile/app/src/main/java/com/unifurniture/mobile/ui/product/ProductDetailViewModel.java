package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.CartRepository;
import com.unifurniture.mobile.data.repository.ProductRepository;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.LiveDataUtil;

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
    private final MutableLiveData<java.util.List<ProductDto>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFavorite = new MutableLiveData<>(false);
    private String wishlistItemId = null;
    private String loadedSlug;

    private final MutableLiveData<String> snackbarMessage = new MutableLiveData<>();

    public ProductDetailViewModel(@NonNull Application application) {
        super(application);
        productRepo = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        cartRepo = new CartRepository(UniFurnitureApp.getInstance().getApiService());
    }

    public void loadProductIfNeeded(String slug) {
        if (slug == null) return;
        if (slug.equals(loadedSlug) && product.getValue() != null) {
            return;
        }
        loadProduct(slug);
    }

    public void loadProduct(String slug) {
        loadedSlug = slug;
        loading.setValue(true);
        LiveDataUtil.observeOnce(productRepo.getProductDetail(slug), p -> {
            product.setValue(p);
            if (p != null) {
                loadImages(p.id);
                loadVariants(p.id);
                loadReviews(p.id);
                loadRecommendations(slug);
                checkIfFavorite(p.id);
            }
            loading.setValue(false);
        });
    }

    private void checkIfFavorite(String productId) {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        
        if (customerId == null) {
            // Check local wishlist for guest
            isFavorite.setValue(session.isInLocalWishlist(productId));
            return;
        }

        LiveDataUtil.observeOnce(productRepo.getWishlist(customerId), list -> {
            if (list != null) {
                for (WishlistItemDto item : list) {
                    if (item.productId != null && item.productId.equals(productId)) {
                        isFavorite.setValue(true);
                        wishlistItemId = item.id;
                        return;
                    }
                }
            }
            // If not found in server wishlist, check if it was recently added locally while guest
            if (session.isInLocalWishlist(productId)) {
                isFavorite.setValue(true);
                wishlistItemId = null;
            } else {
                isFavorite.setValue(false);
                wishlistItemId = null;
            }
        });
    }

    public void toggleWishlist() {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        ProductDto p = product.getValue();
        if (p == null) return;

        if (customerId == null) {
            // Guest mode
            if (Boolean.TRUE.equals(isFavorite.getValue())) {
                session.removeFromLocalWishlist(p.id);
                isFavorite.setValue(false);
                snackbarMessage.setValue(getApplication().getString(R.string.removed_from_favorites));
            } else {
                session.addToLocalWishlist(p.id);
                isFavorite.setValue(true);
                snackbarMessage.setValue(getApplication().getString(R.string.added_to_favorites));
            }
            return;
        }

        // Logged in mode
        if (Boolean.TRUE.equals(isFavorite.getValue())) {
            if (wishlistItemId != null) {
                loading.setValue(true);
                LiveDataUtil.observeOnce(productRepo.removeFromWishlist(wishlistItemId), success -> {
                    loading.setValue(false);
                    if (Boolean.TRUE.equals(success)) {
                        isFavorite.setValue(false);
                        wishlistItemId = null;
                        session.removeFromLocalWishlist(p.id);
                        snackbarMessage.setValue(getApplication().getString(R.string.removed_from_favorites));
                    } else {
                        error.setValue(getApplication().getString(R.string.wishlist_remove_error));
                    }
                });
            } else {
                // If isFavorite is true but wishlistItemId is null, fetch wishlist to resolve the ID
                loading.setValue(true);
                LiveDataUtil.observeOnce(productRepo.getWishlist(customerId), list -> {
                    String foundItemId = null;
                    if (list != null) {
                        for (WishlistItemDto item : list) {
                            if (item.productId != null && item.productId.equals(p.id)) {
                                foundItemId = item.id;
                                break;
                            }
                        }
                    }
                    if (foundItemId != null) {
                        wishlistItemId = foundItemId;
                        LiveDataUtil.observeOnce(productRepo.removeFromWishlist(wishlistItemId), success -> {
                            loading.setValue(false);
                            if (Boolean.TRUE.equals(success)) {
                                isFavorite.setValue(false);
                                wishlistItemId = null;
                                session.removeFromLocalWishlist(p.id);
                                snackbarMessage.setValue(getApplication().getString(R.string.removed_from_favorites));
                            } else {
                                error.setValue(getApplication().getString(R.string.wishlist_remove_error));
                            }
                        });
                    } else {
                        // Not actually in server wishlist, just reset local state
                        loading.setValue(false);
                        isFavorite.setValue(false);
                        session.removeFromLocalWishlist(p.id);
                        snackbarMessage.setValue(getApplication().getString(R.string.removed_from_favorites));
                    }
                });
            }
        } else {
            loading.setValue(true);
            LiveDataUtil.observeOnce(productRepo.addToWishlist(customerId, p.id), item -> {
                loading.setValue(false);
                if (item != null) {
                    isFavorite.setValue(true);
                    wishlistItemId = item.id;
                    session.addToLocalWishlist(p.id);
                    snackbarMessage.setValue(getApplication().getString(R.string.added_to_favorites));
                } else {
                    error.setValue(getApplication().getString(R.string.wishlist_add_error));
                }
            });
        }
    }

    private void loadImages(String productId) {
        LiveDataUtil.observeOnce(productRepo.getProductImages(productId), images::setValue);
    }

    private void loadVariants(String productId) {
        LiveDataUtil.observeOnce(productRepo.getProductVariants(productId), variants::setValue);
    }

    private void loadReviews(String productId) {
        LiveDataUtil.observeOnce(productRepo.getProductReviews(productId), reviews::setValue);
    }

    private void loadRecommendations(String slug) {
        String userId = SessionManager.getInstance(getApplication()).getCustomerId();
        LiveDataUtil.observeOnce(productRepo.getProductRecommendations(slug, userId), recommendations::setValue);
    }

    public void addToCart(String variantId, int quantity) {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        String cartId = session.getCartId();
        ProductDto p = product.getValue();
        error.setValue(null);

        if (p == null) return;

        // Fallback to first variant if none selected
        if (variantId == null || variantId.isEmpty()) {
            ApiListResponse<ProductVariantDto> variantList = variants.getValue();
            if (variantList != null && variantList.items != null && !variantList.items.isEmpty()) {
                variantId = variantList.items.get(0).id;
            }
        }

        if (variantId == null || variantId.isEmpty()) {
            error.setValue(getApplication().getString(R.string.please_select_variant));
            return;
        }

        int finalQuantity = Math.max(1, quantity);

        loading.setValue(true);
        cartRepo.addToCart(customerId, cartId, variantId, finalQuantity, error)
                .observeForever(cart -> {
                    if (cart != null && cart.id != null) {
                        session.saveCartId(cart.id);
                        addToCartResult.setValue(cart);
                        com.unifurniture.mobile.util.CartManager.getInstance().updateCart(cart);
                    }
                    loading.setValue(false);
                });
    }

    public LiveData<ProductDto> getProduct() { return product; }
    public LiveData<ApiListResponse<ProductImageDto>> getImages() { return images; }
    public LiveData<ApiListResponse<ProductVariantDto>> getVariants() { return variants; }
    public LiveData<ReviewSummaryDto> getReviews() { return reviews; }
    public LiveData<CartDto> getAddToCartResult() { return addToCartResult; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<java.util.List<ProductDto>> getRecommendations() { return recommendations; }
    public void clearSnackbarMessage() {
        snackbarMessage.setValue(null);
    }

    public LiveData<Boolean> getIsFavorite() { return isFavorite; }
    public LiveData<String> getSnackbarMessage() { return snackbarMessage; }
}
