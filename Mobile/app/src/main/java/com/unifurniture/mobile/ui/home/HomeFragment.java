package com.unifurniture.mobile.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.data.model.CollectionDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.databinding.FragmentHomeBinding;
import com.unifurniture.mobile.ui.adapter.CategoryAdapter;
import com.unifurniture.mobile.ui.adapter.CollectionAdapter;
import com.unifurniture.mobile.ui.adapter.CouponHomeAdapter;
import com.unifurniture.mobile.ui.adapter.ImageSliderAdapter;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import com.unifurniture.mobile.ui.adapter.RecentlyViewedAdapter;
import com.unifurniture.mobile.ui.adapter.SearchHistoryAdapter;
import com.unifurniture.mobile.ui.adapter.SearchSuggestionAdapter;
import com.unifurniture.mobile.util.RecentlyViewedManager;
import com.unifurniture.mobile.util.SearchHistoryManager;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ProductCardAdapter featuredAdapter;
    private CategoryAdapter categoryAdapter;
    private CollectionAdapter collectionAdapter;
    private ImageSliderAdapter bannerAdapter;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private SearchSuggestionAdapter searchSuggestionAdapter;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private SearchHistoryManager historyManager;
    private SearchHistoryAdapter historyAdapter;
    private RecentlyViewedAdapter recentlyViewedAdapter;
    private RecentlyViewedManager recentlyViewedManager;
    private CouponHomeAdapter couponHomeAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerViews();
        setupBanner();
        setupSearchSuggestions();
        setupSearchHistory();
        setupRecentlyViewed();
        observeData();

        binding.btnViewAll.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.productListFragment));

        binding.btnViewAllCategories.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.categoryFragment));

        // Tint search icon to accent (gold) — same as product list screen
        int accentColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent);
        for (int id : new int[]{
                androidx.appcompat.R.id.search_button,
                androidx.appcompat.R.id.search_mag_icon}) {
            android.widget.ImageView iv = binding.searchView.findViewById(id);
            if (iv != null) {
                iv.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }

        // Use inner EditText for reliable focus detection
        android.widget.EditText searchInnerEdit = binding.searchView.findViewById(
                androidx.appcompat.R.id.search_src_text);
        if (searchInnerEdit != null) {
            searchInnerEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && binding.searchView.getQuery().toString().trim().isEmpty()) {
                    showSearchHistory();
                } else if (!hasFocus) {
                    hideSearchHistory();
                    // Do NOT hideSearchSuggestions() here — focus loss fires before the
                    // suggestion click completes, causing the tap to miss.
                    // Suggestions hide via onQueryTextChange (empty text) or suggestion tap handler.
                }
            });
        }

        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query = query.trim();
                if (!query.isEmpty()) historyManager.add(query);
                hideSearchSuggestions();
                hideSearchHistory();
                Bundle args = new Bundle();
                args.putString("search", query);
                Navigation.findNavController(requireView()).navigate(R.id.productListFragment, args);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                String query = newText.trim();
                if (searchHandler != null && searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                if (query.isEmpty()) {
                    hideSearchSuggestions();
                    showSearchHistory();
                    return true;
                }
                hideSearchHistory();
                searchRunnable = () -> viewModel.searchForSuggestions(query);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadData();
            binding.swipeRefresh.setRefreshing(false);
        });

        // Birthday Popup
        if (com.unifurniture.mobile.util.SessionManager.getInstance(requireContext()).isLoggedIn()) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && getContext() != null) {
                    new android.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.birthday_popup_title))
                        .setMessage(getString(R.string.birthday_popup_message))
                        .setPositiveButton(getString(R.string.birthday_popup_btn),
                                (dialog, which) -> dialog.dismiss())
                        .show();
                }
            }, 3000);
        }
    }

    private void setupBanner() {
        bannerAdapter = new ImageSliderAdapter(requireContext(), new ArrayList<>(), true);
        binding.bannerViewPager.setAdapter(bannerAdapter);
        binding.bannerDotsIndicator.attachTo(binding.bannerViewPager);

        autoScrollHandler = new Handler(Looper.getMainLooper());
        autoScrollRunnable = () -> {
            if (binding == null) return;
            int count = bannerAdapter.getItemCount();
            if (count > 1) {
                int next = (binding.bannerViewPager.getCurrentItem() + 1) % count;
                binding.bannerViewPager.setCurrentItem(next, true);
            }
            autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
        };
    }

    private void setupRecyclerViews() {
        // Featured products - horizontal scroll
        featuredAdapter = new ProductCardAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        featuredAdapter.setColumns(0); // carousel mode — don't force MATCH_PARENT width
        binding.rvFeaturedProducts.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeaturedProducts.setAdapter(featuredAdapter);

        // Categories
        categoryAdapter = new CategoryAdapter(category -> {
            Bundle args = new Bundle();
            args.putString("categoryId", category.id);
            args.putString("categoryName", category.name);
            Navigation.findNavController(requireView()).navigate(R.id.productListFragment, args);
        });
        binding.rvCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);

        // Promotions coupon carousel
        couponHomeAdapter = new CouponHomeAdapter();
        if (binding.rvPromotionCoupons != null) {
            binding.rvPromotionCoupons.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.rvPromotionCoupons.setAdapter(couponHomeAdapter);
        }

        if (binding.btnViewAllPromotions != null) {
            binding.btnViewAllPromotions.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment));
        }

        // Collections
        String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
        collectionAdapter = new CollectionAdapter(serverHost, collection -> {
            Bundle args = new Bundle();
            args.putString("collectionId", collection.id);
            Navigation.findNavController(requireView()).navigate(R.id.productListFragment, args);
        });
        binding.rvCollections.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCollections.setAdapter(collectionAdapter);
    }

    private void observeData() {
        viewModel.getFeaturedProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                featuredAdapter.submitList(response.items);
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                categoryAdapter.submitList(items);
            }
        });

        viewModel.getCollections().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) return;
            String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");

            // Banner slider — extract non-null bannerUrl values
            List<String> bannerUrls = new ArrayList<>();
            for (CollectionDto c : items) {
                if (c.bannerUrl != null && !c.bannerUrl.isEmpty()) {
                    bannerUrls.add(c.bannerUrl.replace("http://localhost:3000", serverHost));
                }
            }
            if (!bannerUrls.isEmpty()) {
                bannerAdapter.updateImages(bannerUrls);
                binding.bannerDotsIndicator.setVisibility(
                        bannerUrls.size() > 1 ? View.VISIBLE : View.GONE);
                if (bannerUrls.size() > 1) {
                    autoScrollHandler.removeCallbacks(autoScrollRunnable);
                    autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
                }
            }

            // Collections row
            collectionAdapter.submitList(items);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                binding.shimmerLayout.setVisibility(View.VISIBLE);
                binding.shimmerLayout.startShimmer();
                binding.layoutContent.setVisibility(View.GONE);
            } else {
                binding.shimmerLayout.stopShimmer();
                binding.shimmerLayout.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getCoupons().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty() && couponHomeAdapter != null) {
                couponHomeAdapter.submitList(items);
                if (binding.layoutPromotions != null) {
                    binding.layoutPromotions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getSearchSuggestions().observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                searchSuggestionAdapter.submitList(products);
                binding.rvSearchSuggestions.setVisibility(View.VISIBLE);
            } else {
                hideSearchSuggestions();
            }
        });
    }

    private void setupSearchSuggestions() {
        searchHandler = new Handler(Looper.getMainLooper());
        binding.rvSearchSuggestions.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        searchSuggestionAdapter = new SearchSuggestionAdapter(product -> {
            hideSearchSuggestions();
            binding.searchView.setQuery("", false);
            String slug = product.slug != null ? product.slug : product.id;
            Bundle args = new Bundle();
            args.putString("slug", slug);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        binding.rvSearchSuggestions.setAdapter(searchSuggestionAdapter);
    }

    private void hideSearchSuggestions() {
        binding.rvSearchSuggestions.setVisibility(View.GONE);
    }

    private void setupRecentlyViewed() {
        recentlyViewedManager = new RecentlyViewedManager(requireContext());
        recentlyViewedAdapter = new RecentlyViewedAdapter(item -> {
            Bundle args = new Bundle();
            args.putString("slug", item.slug != null ? item.slug : item.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        binding.rvRecentlyViewed.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecentlyViewed.setAdapter(recentlyViewedAdapter);
    }

    private void refreshRecentlyViewed() {
        List<RecentlyViewedManager.Item> items = recentlyViewedManager.getAll();
        if (items.isEmpty()) {
            binding.layoutRecentlyViewed.setVisibility(View.GONE);
        } else {
            recentlyViewedAdapter.submitList(items);
            binding.layoutRecentlyViewed.setVisibility(View.VISIBLE);
        }
    }

    private void setupSearchHistory() {
        historyManager = new SearchHistoryManager(requireContext());
        historyAdapter = new SearchHistoryAdapter(new SearchHistoryAdapter.OnItemListener() {
            @Override
            public void onQueryClick(String query) {
                hideSearchHistory();
                binding.searchView.setQuery(query, true); // true = submit immediately
            }
            @Override
            public void onDeleteClick(String query) {
                historyManager.remove(query);
                refreshHistory();
            }
        });
        binding.rvSearchHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchHistory.setAdapter(historyAdapter);

        binding.tvClearHistory.setOnClickListener(v -> {
            historyManager.clear();
            hideSearchHistory();
        });
    }

    private void showSearchHistory() {
        List<String> history = historyManager.getAll();
        if (history.isEmpty()) {
            hideSearchHistory();
            return;
        }
        historyAdapter.submitList(history);
        binding.layoutSearchHistory.setVisibility(View.VISIBLE);
    }

    private void hideSearchHistory() {
        binding.layoutSearchHistory.setVisibility(View.GONE);
    }

    private void refreshHistory() {
        List<String> history = historyManager.getAll();
        if (history.isEmpty()) {
            hideSearchHistory();
        } else {
            historyAdapter.submitList(new ArrayList<>(history));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerAdapter != null && bannerAdapter.getItemCount() > 1) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
        }
        if (recentlyViewedManager != null) refreshRecentlyViewed();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (autoScrollHandler != null) autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autoScrollHandler != null) autoScrollHandler.removeCallbacks(autoScrollRunnable);
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        binding = null;
    }
}
