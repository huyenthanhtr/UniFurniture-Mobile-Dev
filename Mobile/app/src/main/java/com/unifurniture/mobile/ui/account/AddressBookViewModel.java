package com.unifurniture.mobile.ui.account;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CustomerAddressDto;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.util.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressBookViewModel extends AndroidViewModel {

    private final ApiService apiService;
    private final MutableLiveData<List<CustomerAddressDto>> addresses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AddressBookViewModel(@NonNull Application application) {
        super(application);
        apiService = UniFurnitureApp.getInstance().getApiService();
    }

    public void loadAddressesIfNeeded() {
        if (addresses.getValue() != null) {
            return;
        }
        loadAddresses(true);
    }

    public void reloadAddresses() {
        loadAddresses(false);
    }

    private void loadAddresses(boolean showLoading) {
        String customerId = SessionManager.getInstance(getApplication()).getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            addresses.setValue(List.of());
            return;
        }
        if (showLoading) loading.setValue(true);
        apiService.getCustomerAddresses(customerId, 100).enqueue(new Callback<ApiListResponse<CustomerAddressDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiListResponse<CustomerAddressDto>> call,
                                   @NonNull Response<ApiListResponse<CustomerAddressDto>> response) {
                List<CustomerAddressDto> items = response.body() != null ? response.body().getData() : null;
                addresses.setValue(response.isSuccessful() && items != null ? items : List.of());
                loading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<ApiListResponse<CustomerAddressDto>> call, @NonNull Throwable t) {
                errorMessage.setValue(t.getMessage());
                loading.setValue(false);
            }
        });
    }

    public LiveData<List<CustomerAddressDto>> getAddresses() { return addresses; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}
