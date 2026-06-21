package com.unifurniture.mobile.ui.cart;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CouponDto;
import com.unifurniture.mobile.data.repository.ProductRepository;
import java.util.List;

public class VoucherListViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final MutableLiveData<List<CouponDto>> coupons = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public VoucherListViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
    }

    public void loadCouponsIfNeeded() {
        if (coupons.getValue() != null) {
            return;
        }
        loading.setValue(true);
        repository.getCoupons().observeForever(list -> {
            coupons.setValue(list);
            loading.setValue(false);
        });
    }

    public LiveData<List<CouponDto>> getCoupons() { return coupons; }
    public LiveData<Boolean> isLoading() { return loading; }
}
