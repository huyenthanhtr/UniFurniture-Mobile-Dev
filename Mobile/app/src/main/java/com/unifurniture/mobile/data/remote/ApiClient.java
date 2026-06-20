package com.unifurniture.mobile.data.remote;

import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.util.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static ApiService INSTANCE;

    public static ApiService getInstance() {
        if (INSTANCE == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        SessionManager session = SessionManager.getInstance(UniFurnitureApp.getInstance());
                        String token = session.getToken();
                        
                        Request.Builder builder = original.newBuilder();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        
                        // Log the request for debugging
                        android.util.Log.d("ApiClient", "Request: " + original.method() + " " + original.url());
                        
                        return chain.proceed(builder.build());
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            INSTANCE = retrofit.create(ApiService.class);
        }
        return INSTANCE;
    }

    public static ApiService getClient() {
        return getInstance();
    }

    private ApiClient() {}
}
