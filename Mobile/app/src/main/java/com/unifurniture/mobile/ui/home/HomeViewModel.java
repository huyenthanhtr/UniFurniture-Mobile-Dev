package com.unifurniture.mobile.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.*;
import com.unifurniture.mobile.data.repository.ProductRepository;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.LanguageHelper;
import com.unifurniture.mobile.util.LiveDataUtil;
import com.unifurniture.mobile.util.RecentlyViewedManager;
import com.unifurniture.mobile.util.SessionManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    // Language the currently-cached data was loaded for; used to detect a switch and refetch.
    private String loadedLang;
    private final MutableLiveData<ApiListResponse<ProductDto>> featuredProducts = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<CollectionDto>> collections = new MutableLiveData<>();
    private final MutableLiveData<List<CouponDto>> coupons = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ProductDto>> searchSuggestions = new MutableLiveData<>();
    // "You may be interested" — recommendations seeded by recently-viewed + purchased products.
    private final MutableLiveData<List<ProductDto>> recommended = new MutableLiveData<>();
    private final List<ProductDto> allProductsCache = new ArrayList<>();

    // Track observers so we can remove them in onCleared()
    private LiveData<ApiListResponse<ProductDto>> featuredLiveData;
    private LiveData<List<CategoryDto>> categoriesLiveData;
    private LiveData<List<CollectionDto>> collectionsLiveData;
    private LiveData<List<CouponDto>> couponsLiveData;
    private LiveData<ApiListResponse<ProductDto>> suggestionsLiveData;
    private Observer<ApiListResponse<ProductDto>> suggestionsObserver;

    private final Observer<ApiListResponse<ProductDto>> featuredObserver = r -> {
        featuredProducts.setValue(r);
        if (r != null && r.items != null) {
            // Update cache with featured products
            for (ProductDto p : r.items) {
                if (!allProductsCache.contains(p)) allProductsCache.add(p);
            }
        }
        loading.setValue(false);
    };
    private final Observer<List<CategoryDto>> categoriesObserver = categories::setValue;
    private final Observer<List<CollectionDto>> collectionsObserver = collections::setValue;
    private final Observer<List<CouponDto>> couponsObserver = coupons::setValue;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(UniFurnitureApp.getInstance().getApiService());
        if (featuredProducts.getValue() == null) {
            loadData();
        }
    }

    private void loadDefaultFeaturedProducts() {
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        featuredLiveData = repository.getProducts(1, 10, null, null, null, "createdAt", "desc", null, null, null);
        featuredLiveData.observeForever(featuredObserver);
    }

    public void loadData() {
        if (featuredProducts.getValue() != null) {
            return;
        }
        loadedLang = LanguageHelper.getLanguage(getApplication());
        loading.setValue(true);

        // Remove old observers before re-subscribing
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);

        // Load the default featured list right away for a fast first paint. On-device
        // recommendations are applied afterwards in personalizeFeatured() (async, no extra fetch).
        loadDefaultFeaturedProducts();

        categoriesLiveData = repository.getCategories();
        categoriesLiveData.observeForever(categoriesObserver);

        collectionsLiveData = repository.getCollections();
        collectionsLiveData.observeForever(collectionsObserver);

        couponsLiveData = repository.getCoupons();
        couponsLiveData.observeForever(couponsObserver);
    }

    /**
     * Recently-viewed entries cache the product name in the language it was viewed in. When the
     * app language differs, refetch those names (via the lang-aware API) so the section follows
     * the current language. Only stale-language items are fetched; calls back when names update.
     */
    public void refreshRecentlyViewedIfStale(Runnable onUpdated) {
        RecentlyViewedManager mgr = new RecentlyViewedManager(getApplication());
        List<RecentlyViewedManager.Item> items = mgr.getAll();
        String currentLang = LanguageHelper.getLanguage(getApplication());

        List<RecentlyViewedManager.Item> stale = new ArrayList<>();
        for (RecentlyViewedManager.Item it : items) {
            if (it.slug != null && (it.lang == null || !it.lang.equals(currentLang))) stale.add(it);
        }
        if (stale.isEmpty()) return;

        final int[] remaining = { stale.size() };
        for (RecentlyViewedManager.Item it : stale) {
            LiveDataUtil.observeOnce(repository.getProductDetail(it.slug), p -> {
                if (p != null && p.name != null) {
                    it.name = p.name;
                    it.lang = currentLang;
                }
                if (--remaining[0] == 0) {
                    mgr.saveAll(items);
                    if (onUpdated != null) onUpdated.run();
                }
            });
        }
    }

    /**
     * Build the "You may be interested" list. Seeds are the products the user recently viewed plus,
     * when logged in, the products they recently purchased. We reuse the server's content-based
     * recommendation endpoint for the top seeds and merge the results (excluding items already
     * viewed/purchased). Guests with no history get an empty list (section is hidden).
     */
    public void loadRecommended() {
        RecentlyViewedManager mgr = new RecentlyViewedManager(getApplication());
        List<String> seeds = new ArrayList<>();
        Set<String> excludeIds = new LinkedHashSet<>();
        for (RecentlyViewedManager.Item it : mgr.getAll()) {
            if (it.slug != null) seeds.add(it.slug);
            if (it.id != null) excludeIds.add(it.id);
        }

        String customerId = SessionManager.getInstance(getApplication()).getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            fetchRecommendations(seeds, excludeIds, null);
            return;
        }

        // Logged in → fold in purchase history, then fetch.
        UniFurnitureApp.getInstance().getApiService().getOrders(customerId, null)
                .enqueue(new Callback<ApiListResponse<OrderDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiListResponse<OrderDto>> call,
                                           @NonNull Response<ApiListResponse<OrderDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            for (OrderDto order : response.body().items) {
                                if (order.getDetails() == null) continue;
                                for (OrderDetailDto d : order.getDetails()) {
                                    if (d.getProductId() != null) excludeIds.add(d.getProductId());
                                    if (d.getProduct() != null && d.getProduct().slug != null) {
                                        seeds.add(0, d.getProduct().slug); // purchases are stronger signals
                                    }
                                }
                            }
                        }
                        fetchRecommendations(seeds, excludeIds, customerId);
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiListResponse<OrderDto>> call, @NonNull Throwable t) {
                        fetchRecommendations(seeds, excludeIds, customerId);
                    }
                });
    }

    private void fetchRecommendations(List<String> seeds, Set<String> excludeIds, String userId) {
        List<String> uniqueSeeds = new ArrayList<>(new LinkedHashSet<>(seeds));
        if (uniqueSeeds.size() > 2) uniqueSeeds = uniqueSeeds.subList(0, 2); // top 2 seeds is plenty
        if (uniqueSeeds.isEmpty()) {
            recommended.setValue(new ArrayList<>());
            return;
        }
        LinkedHashMap<String, ProductDto> merged = new LinkedHashMap<>();
        final int[] remaining = { uniqueSeeds.size() };
        for (String slug : uniqueSeeds) {
            LiveDataUtil.observeOnce(repository.getProductRecommendations(slug, userId), list -> {
                if (list != null) {
                    for (ProductDto p : list) {
                        if (p.id != null && !excludeIds.contains(p.id) && !merged.containsKey(p.id)) {
                            merged.put(p.id, p);
                        }
                    }
                }
                if (--remaining[0] == 0) {
                    List<ProductDto> out = new ArrayList<>(merged.values());
                    if (out.size() > 10) out = out.subList(0, 10);
                    recommended.setValue(out);
                }
            });
        }
    }

    /** If the UI language changed since the cached data was loaded, refetch in the new language. */
    public void reloadIfLanguageChanged() {
        String current = LanguageHelper.getLanguage(getApplication());
        if (loadedLang != null && !loadedLang.equals(current)) {
            refreshData();
        }
    }

    /** Pull-to-refresh: always fetch fresh data. */
    public void refreshData() {
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);
        featuredProducts.setValue(null);
        loadData();
    }

    private List<ProductDto> sortByRelevance(List<ProductDto> list, String query) {
        if (list == null || query == null || query.trim().isEmpty()) return list;
        String nQuery = FormatUtil.stripDiacritics(query.trim().toLowerCase());
        List<ProductDto> sorted = new ArrayList<>(list);
        sorted.sort((p1, p2) -> {
            String n1 = FormatUtil.stripDiacritics(p1.name != null ? p1.name.toLowerCase() : "");
            String n2 = FormatUtil.stripDiacritics(p2.name != null ? p2.name.toLowerCase() : "");

            boolean start1 = n1.startsWith(nQuery);
            boolean start2 = n2.startsWith(nQuery);
            if (start1 && !start2) return -1;
            if (!start1 && start2) return 1;

            boolean wordStart1 = n1.contains(" " + nQuery);
            boolean wordStart2 = n2.contains(" " + nQuery);
            if (wordStart1 && !wordStart2) return -1;
            if (!wordStart1 && wordStart2) return 1;

            return n1.compareTo(n2);
        });
        return sorted;
    }

    public void searchForSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchSuggestions.setValue(null);
            return;
        }

        String q = query.trim();
        // Instant local filter for snappy UI
        List<ProductDto> localMatches = new ArrayList<>();
        String normalizedQuery = FormatUtil.stripDiacritics(q.toLowerCase());
        for (ProductDto p : allProductsCache) {
            if (p.name != null) {
                String normalizedName = FormatUtil.stripDiacritics(p.name.toLowerCase());
                if (normalizedName.contains(normalizedQuery)) {
                    localMatches.add(p);
                }
            }
        }

        if (!localMatches.isEmpty()) {
            searchSuggestions.setValue(sortByRelevance(localMatches, q));
        }

        // Remove previous suggestion observer before creating a new one
        if (suggestionsLiveData != null && suggestionsObserver != null) {
            suggestionsLiveData.removeObserver(suggestionsObserver);
        }
        suggestionsObserver = response -> {
            if (response != null && response.items != null) {
                // Update cache with results from server
                for (ProductDto p : response.items) {
                    boolean exists = false;
                    for (ProductDto cached : allProductsCache) {
                        if (cached.id.equals(p.id)) { exists = true; break; }
                    }
                    if (!exists) allProductsCache.add(p);
                }
                // Always update suggestions if server returns something, sorted by relevance
                if (!response.items.isEmpty()) {
                    searchSuggestions.setValue(sortByRelevance(response.items, q));
                } else if (localMatches.isEmpty()) {
                    searchSuggestions.setValue(new ArrayList<>()); // Clear if nothing found
                }
            }
        };
        suggestionsLiveData = repository.getProducts(
                1, 8, q, null, null, "createdAt", "desc", null, null, null);
        suggestionsLiveData.observeForever(suggestionsObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (featuredLiveData != null) featuredLiveData.removeObserver(featuredObserver);
        if (categoriesLiveData != null) categoriesLiveData.removeObserver(categoriesObserver);
        if (collectionsLiveData != null) collectionsLiveData.removeObserver(collectionsObserver);
        if (couponsLiveData != null) couponsLiveData.removeObserver(couponsObserver);
        if (suggestionsLiveData != null && suggestionsObserver != null)
            suggestionsLiveData.removeObserver(suggestionsObserver);
    }

    public LiveData<ApiListResponse<ProductDto>> getFeaturedProducts() { return featuredProducts; }
    public LiveData<List<CategoryDto>> getCategories() { return categories; }
    public LiveData<List<CollectionDto>> getCollections() { return collections; }
    public LiveData<List<CouponDto>> getCoupons() { return coupons; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<List<ProductDto>> getSearchSuggestions() { return searchSuggestions; }
    public LiveData<List<ProductDto>> getRecommended() { return recommended; }
}
