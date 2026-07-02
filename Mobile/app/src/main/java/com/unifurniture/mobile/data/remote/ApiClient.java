package com.unifurniture.mobile.data.remote;

import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static ApiService INSTANCE;
    private static OkHttpClient RAW_CLIENT;

    public static ApiService getInstance() {
        if (INSTANCE == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Verbose body logging only in debug; release stays quiet and avoids leaking payloads.
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            // Disk cache so revisiting a screen (products / categories / collections) serves
            // instantly instead of re-hitting the network on every navigation.
            okhttp3.Cache httpCache = new okhttp3.Cache(
                    new java.io.File(UniFurnitureApp.getInstance().getCacheDir(), "http_cache"),
                    10L * 1024 * 1024); // 10 MB

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cache(httpCache)
                    .addInterceptor(logging)
                    // Network interceptor: make public catalog GETs cacheable for a short window so
                    // repeated navigations are instant (the backend doesn't send Cache-Control).
                    .addNetworkInterceptor(chain -> {
                        okhttp3.Request req = chain.request();
                        String p = req.url().encodedPath();
                        boolean cacheableGet = "GET".equals(req.method())
                                && ((p.contains("/products") && !p.contains("/product-images") && !p.contains("/product-variants"))
                                    || p.contains("/categories")
                                    || p.contains("/collections"));
                        okhttp3.Response resp = chain.proceed(req);
                        if (cacheableGet && resp.isSuccessful()) {
                            return resp.newBuilder()
                                    .removeHeader("Pragma")
                                    .header("Cache-Control", "public, max-age=60")
                                    .build();
                        }
                        return resp;
                    })
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        SessionManager session = SessionManager.getInstance(UniFurnitureApp.getInstance());
                        String token = session.getToken();

                        Request.Builder builder = original.newBuilder();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        // Append the current UI language as ?lang= so the backend can return
                        // translated catalog/blog/order item content.
                        // Only add it to GET endpoints that support translation and don't fail on extra query params.
                        String lang = com.unifurniture.mobile.util.LanguageHelper
                                .getLanguage(UniFurnitureApp.getInstance());
                        if (lang != null && !lang.isEmpty()) {
                            builder.header("Accept-Language", lang);
                        }
                        String path = original.url().encodedPath();
                        boolean shouldAppendLang = "GET".equals(original.method())
                                && ((path.contains("/products") && !path.contains("/product-images") && !path.contains("/product-variants"))
                                    || path.contains("/categories")
                                    || path.contains("/collections")
                                    || path.contains("/orders")
                                    || path.contains("/posts"));

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
            RAW_CLIENT = okHttpClient;

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ReviewDto.class, new ReviewDto.Deserializer())
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            INSTANCE = retrofit.create(ApiService.class);
        }
        return INSTANCE;
    }

    public static ApiService getClient() {
        return getInstance();
    }

    public static OkHttpClient getRawClient() {
        getInstance();
        return RAW_CLIENT;
    }

    private ApiClient() {}
}
