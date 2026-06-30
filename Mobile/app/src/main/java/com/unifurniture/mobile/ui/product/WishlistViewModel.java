package com.unifurniture.mobile.ui.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.WishlistItemDto;
import com.unifurniture.mobile.data.repository.ProductRepository;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.LiveDataUtil;
import java.util.List;

public class WishlistViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<List<WishlistItemDto>> wishlist = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public WishlistViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
    }

    public void loadWishlistIfNeeded() {
        if (wishlist.getValue() != null && !Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loadWishlist();
    }

    public void loadWishlist() {
        SessionManager session = SessionManager.getInstance(getApplication());
        String customerId = session.getCustomerId();
        
        if (customerId == null) {
            // Check local wishlist for guest
            java.util.Set<String> localIds = session.getLocalWishlist();
            if (localIds.isEmpty()) {
                wishlist.setValue(new java.util.ArrayList<>());
                return;
            }
            
            loading.setValue(true);
            java.util.List<WishlistItemDto> localItems = new java.util.ArrayList<>();
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(localIds.size());
            
            for (String id : localIds) {
                LiveDataUtil.observeOnce(repository.getProductDetail(id), product -> {
                    if (product != null) {
                        WishlistItemDto item = new WishlistItemDto();
                        item.id = "local_" + id;
                        item.productId = id;
                        item.product = product;
                        localItems.add(item);
                    }
                    if (counter.decrementAndGet() == 0) {
                        wishlist.setValue(localItems);
                        loading.setValue(false);
                    }
                });
            }
            return;
        }

        loading.setValue(true);
        LiveDataUtil.observeOnce(repository.getWishlist(customerId), items -> {
            wishlist.setValue(items);
            loading.setValue(false);
        });
    }

    public void removeFromWishlist(WishlistItemDto item) {
        if (item == null) return;
        String productId = item.productId;

        // Guest / locally-added items are not on the server.
        if (item.id != null && item.id.startsWith("local_")) {
            SessionManager.getInstance(getApplication()).removeFromLocalWishlist(productId);
            loadWishlist();
            return;
        }

        SessionManager session = SessionManager.getInstance(getApplication());
        String profileId = session.getCustomerId();
        if (profileId == null) {
            session.removeFromLocalWishlist(productId);
            loadWishlist();
            return;
        }

        LiveDataUtil.observeOnce(repository.removeFromWishlist(profileId, productId), success -> {
            if (Boolean.TRUE.equals(success)) {
                loadWishlist(); // Reload after removal
            } else {
                error.setValue(getApplication().getString(com.unifurniture.mobile.R.string.wishlist_remove_error));
            }
        });
    }

    public LiveData<List<WishlistItemDto>> getWishlist() { return wishlist; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
