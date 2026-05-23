package com.unifurniture.mobile.ui.cart;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CartDto;
import com.unifurniture.mobile.data.repository.CartRepository;
import com.unifurniture.mobile.util.SessionManager;

public class CartViewModel extends AndroidViewModel {

    private final CartRepository repository;
    private final MutableLiveData<CartDto> cart = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepository(UniFurnitureApp.getInstance().getApiService());
        loadCart();
    }

    public void loadCart() {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        if (customerId == null) {
            cart.setValue(null);
            return;
        }
        loading.setValue(true);
        repository.getActiveCart(customerId).observeForever(c -> {
            cart.setValue(c);
            loading.setValue(false);
        });
    }

    public void updateQuantity(String cartItemId, int quantity) {
        if (quantity <= 0) {
            removeItem(cartItemId);
            return;
        }
        repository.updateCartItemQuantity(cartItemId, quantity)
                .observeForever(cart::setValue);
    }

    public void removeItem(String cartItemId) {
        repository.removeCartItem(cartItemId).observeForever(cart::setValue);
    }

    public double getTotal() {
        CartDto c = cart.getValue();
        if (c == null || c.items == null) return 0;
        double total = 0;
        for (var item : c.items) total += item.getTotalPrice();
        return total;
    }

    public LiveData<CartDto> getCart() { return cart; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
