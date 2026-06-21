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
            // Verbose body logging only in debug; release stays quiet and avoids leaking payloads.
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

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

                        // Append the current UI language as ?lang= so the backend can return
                        // translated product content (name/short_description/description).
                        // Only add it to endpoints that support translation and don't fail on extra query params.
                        String lang = com.unifurniture.mobile.util.LanguageHelper
                                .getLanguage(UniFurnitureApp.getInstance());
                        String path = original.url().encodedPath();
                        boolean shouldAppendLang = (path.contains("/products") && !path.contains("/product-images") && !path.contains("/product-variants"))
                                || path.contains("/categories")
                                || path.contains("/collections");

                        if (shouldAppendLang && lang != null && !lang.isEmpty() && original.url().queryParameter("lang") == null) {
                            builder.url(original.url().newBuilder()
                                    .addQueryParameter("lang", lang)
                                    .build());
                        }

                        // Always log request line (kept lightweight) so release-build failures are traceable.
                        android.util.Log.d("ApiClient", "Request: " + original.method() + " " + original.url());

                        try {
                            okhttp3.Response response = chain.proceed(builder.build());
                            if (!response.isSuccessful()) {
                                android.util.Log.w("ApiClient", "HTTP " + response.code()
                                        + " for " + original.method() + " " + original.url());
                            }
                            return response;
                        } catch (java.io.IOException e) {
                            // Network/DNS/TLS failure — the usual cause of empty screens on real devices.
                            android.util.Log.e("ApiClient", "Request FAILED: " + original.method()
                                    + " " + original.url() + " baseUrl=" + BuildConfig.API_BASE_URL, e);
                            throw e;
                        }
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
