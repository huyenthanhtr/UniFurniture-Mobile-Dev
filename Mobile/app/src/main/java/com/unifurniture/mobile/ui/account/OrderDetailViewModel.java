package com.unifurniture.mobile.ui.account;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.OrderDetailDto;
import com.unifurniture.mobile.data.model.OrderDetailResponse;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.data.remote.ApiService;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailViewModel extends AndroidViewModel {

    public static class OrderDetailState {
        public final OrderDto order;
        public final List<OrderDetailDto> items;

        public OrderDetailState(OrderDto order, List<OrderDetailDto> items) {
            this.order = order;
            this.items = items;
        }
    }

    private final ApiService apiService;
    private final MutableLiveData<OrderDetailState> orderDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private String loadedOrderId;

    public OrderDetailViewModel(@NonNull Application application) {
        super(application);
        apiService = UniFurnitureApp.getInstance().getApiService();
    }

    public void loadOrderIfNeeded(String orderId) {
        if (orderId == null) return;
        if (orderId.equals(loadedOrderId) && orderDetail.getValue() != null) {
            return;
        }
        loadedOrderId = orderId;
        loading.setValue(true);
        apiService.getOrderById(orderId).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderDetailResponse> call,
                                   @NonNull Response<OrderDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getOrder() != null) {
                    orderDetail.setValue(new OrderDetailState(
                            response.body().getOrder(), response.body().getItems()));
                } else {
                    orderDetail.setValue(null);
                }
                loading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<OrderDetailResponse> call, @NonNull Throwable t) {
                orderDetail.setValue(null);
                loading.setValue(false);
            }
        });
    }

    public LiveData<OrderDetailState> getOrderDetail() { return orderDetail; }
    public LiveData<Boolean> isLoading() { return loading; }
}
