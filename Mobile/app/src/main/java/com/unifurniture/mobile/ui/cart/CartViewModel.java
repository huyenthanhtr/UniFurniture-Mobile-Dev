package com.unifurniture.mobile.ui.cart;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CartDto;
import com.unifurniture.mobile.data.repository.CartRepository;
import com.unifurniture.mobile.util.CartManager;
import com.unifurniture.mobile.util.LiveDataUtil;
import com.unifurniture.mobile.util.SessionManager;

public class CartViewModel extends AndroidViewModel {

    private final CartRepository repository;
    private final CartManager cartManager = CartManager.getInstance();
    private final MutableLiveData<CartDto> cart = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final Observer<CartDto> sharedCartObserver = sharedCart -> {
        if (sharedCart != null) {
            cart.setValue(sharedCart);
        }
    };

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepository(UniFurnitureApp.getInstance().getApiService());
        cartManager.getCart().observeForever(sharedCartObserver);
        CartDto cachedCart = cartManager.getCurrentCart();
        if (cachedCart != null) {
            cart.setValue(cachedCart);
        }
        loadCartIfNeeded();
    }

    public void loadCartIfNeeded() {
        if (cart.getValue() != null) {
            return;
        }
        loadCart();
    }

    public void loadCart() {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        String cartId = session.getCartId();
        
        if (customerId == null && cartId == null) {
            CartDto cachedCart = cartManager.getCurrentCart();
            if (cachedCart != null) {
                cart.setValue(cachedCart);
            } else {
                cart.setValue(null);
            }
            return;
        }

        boolean showLoading = cart.getValue() == null;
        if (showLoading) {
            loading.setValue(true);
        }
        LiveDataUtil.observeOnce(repository.getActiveCart(customerId, cartId), c -> {
            if (c != null && c.id != null) {
                session.saveCartId(c.id);
                cart.setValue(c);
                cartManager.updateCart(c);
            } else if (cart.getValue() == null) {
                cart.setValue(cartManager.getCurrentCart());
            }
            loading.setValue(false);
        });
    }

    public void updateQuantity(String cartItemId, int quantity) {
        if (cartItemId == null || cartItemId.isEmpty()) {
            return;
        }
        if (quantity <= 0) {
            removeItem(cartItemId);
            return;
        }
        LiveDataUtil.observeOnce(repository.updateCartItemQuantity(cartItemId, quantity), c -> {
                    if (c != null && c.id != null) {
                        SessionManager.getInstance(getApplication()).saveCartId(c.id);
                        cart.setValue(c);
                        cartManager.updateCart(c);
                    }
                });
    }

    public void removeItem(String cartItemId) {
        if (cartItemId == null || cartItemId.isEmpty()) {
            return;
        }
        LiveDataUtil.observeOnce(repository.removeCartItem(cartItemId), c -> {
            if (c != null && c.id != null) {
                SessionManager.getInstance(getApplication()).saveCartId(c.id);
                cart.setValue(c);
                cartManager.updateCart(c);
            }
        });
    }

    public void updateCartItemVariant(String cartItemId, String variantId) {
        if (cartItemId == null || cartItemId.isEmpty() || variantId == null || variantId.isEmpty()) {
            return;
        }
        loading.setValue(true);
        LiveDataUtil.observeOnce(repository.updateCartItemVariant(cartItemId, variantId), c -> {
            if (c != null && c.id != null) {
                SessionManager.getInstance(getApplication()).saveCartId(c.id);
                cart.setValue(c);
                cartManager.updateCart(c);
            }
            loading.setValue(false);
        });
    }

    public LiveData<java.util.List<com.unifurniture.mobile.data.model.ProductVariantDto>> getProductVariants(String productId) {
        MutableLiveData<java.util.List<com.unifurniture.mobile.data.model.ProductVariantDto>> result = new MutableLiveData<>();
        if (productId == null || productId.isEmpty()) {
            result.setValue(new java.util.ArrayList<>());
            return result;
        }
        UniFurnitureApp.getInstance().getApiService().getProductVariants(productId, "active", 100)
                .enqueue(new retrofit2.Callback<com.unifurniture.mobile.data.model.ApiListResponse<com.unifurniture.mobile.data.model.ProductVariantDto>>() {
                    @Override
                    public void onResponse(
                            @NonNull retrofit2.Call<com.unifurniture.mobile.data.model.ApiListResponse<com.unifurniture.mobile.data.model.ProductVariantDto>> call,
                            @NonNull retrofit2.Response<com.unifurniture.mobile.data.model.ApiListResponse<com.unifurniture.mobile.data.model.ProductVariantDto>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body().items);
                        } else {
                            result.setValue(new java.util.ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull retrofit2.Call<com.unifurniture.mobile.data.model.ApiListResponse<com.unifurniture.mobile.data.model.ProductVariantDto>> call,
                            @NonNull Throwable t
                    ) {
                        result.setValue(new java.util.ArrayList<>());
                    }
                });
        return result;
    }

    public double getTotal() {
        CartDto c = cart.getValue();
        if (c == null || c.items == null) return 0;
        double total = 0;
        for (var item : c.items) {
            if (item != null) {
                total += item.getTotalPrice();
            }
        }
        return total;
    }

    public LiveData<CartDto> getCart() { return cart; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    @Override
    protected void onCleared() {
        cartManager.getCart().removeObserver(sharedCartObserver);
        super.onCleared();
    }
}
