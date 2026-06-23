package com.unifurniture.mobile.util;

import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.data.model.CartDto;
import com.unifurniture.mobile.data.model.CartItemDto;

public class CartManager {
    private static CartManager instance;
    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);
    private final MutableLiveData<CartDto> cart = new MutableLiveData<>();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public LiveData<Integer> getCartCount() {
        return cartCount;
    }

    public LiveData<CartDto> getCart() {
        return cart;
    }

    public CartDto getCurrentCart() {
        return cart.getValue();
    }

    public void updateCart(CartDto cart) {
        setCartValue(cart);

        if (cart == null || cart.items == null) {
            setCartCountValue(0);
            return;
        }
        int count = 0;
        for (CartItemDto item : cart.items) {
            if (item != null && item.quantity != null) {
                count += item.quantity;
            }
        }
        setCartCountValue(count);
    }

    private void setCartValue(CartDto value) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cart.setValue(value);
        } else {
            cart.postValue(value);
        }
    }

    private void setCartCountValue(int value) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cartCount.setValue(value);
        } else {
            cartCount.postValue(value);
        }
    }
}
