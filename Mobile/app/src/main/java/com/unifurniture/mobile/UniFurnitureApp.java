package com.unifurniture.mobile;

import android.app.Application;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;

public class UniFurnitureApp extends Application {

    private static UniFurnitureApp instance;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        apiService = ApiClient.getInstance();
        com.unifurniture.mobile.messaging.NotificationChannels.createAll(this);
    }

    public static UniFurnitureApp getInstance() {
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
