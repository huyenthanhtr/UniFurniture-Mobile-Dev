package com.unifurniture.mobile.ui.account;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.util.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReviewsViewModel extends AndroidViewModel {

    private final ApiService apiService;
    private final MutableLiveData<List<ReviewDto>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private String loadedCustomerId;

    public MyReviewsViewModel(@NonNull Application application) {
        super(application);
        apiService = UniFurnitureApp.getInstance().getApiService();
    }

    public void loadReviewsIfNeeded() {
        String customerId = SessionManager.getInstance(getApplication()).getCustomerId();
        if (customerId == null) return;
        if (customerId.equals(loadedCustomerId) && reviews.getValue() != null) {
            return;
        }
        loadedCustomerId = customerId;
        loading.setValue(true);
        apiService.getReviews(customerId).enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReviewDto>> call, @NonNull Response<List<ReviewDto>> response) {
                reviews.setValue(response.isSuccessful() && response.body() != null
                        ? response.body() : List.of());
                loading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<List<ReviewDto>> call, @NonNull Throwable t) {
                reviews.setValue(null);
                loading.setValue(false);
            }
        });
    }

    public LiveData<List<ReviewDto>> getReviews() { return reviews; }
    public LiveData<Boolean> isLoading() { return loading; }
}
