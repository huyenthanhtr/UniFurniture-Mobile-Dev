package com.unifurniture.mobile.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CollectionDto;
import com.unifurniture.mobile.data.model.NotificationDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.databinding.FragmentHomeBinding;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.ui.adapter.CategoryAdapter;
import com.unifurniture.mobile.ui.adapter.CollectionAdapter;
import com.unifurniture.mobile.ui.adapter.CouponHomeAdapter;
import com.unifurniture.mobile.ui.adapter.ImageSliderAdapter;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import com.unifurniture.mobile.ui.adapter.RecentlyViewedAdapter;
import com.unifurniture.mobile.ui.adapter.SearchHistoryAdapter;
import com.unifurniture.mobile.ui.adapter.SearchSuggestionAdapter;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.NotificationManager;
import com.unifurniture.mobile.util.RecentlyViewedManager;
import com.unifurniture.mobile.util.ScrollStateHelper;
import com.unifurniture.mobile.util.SearchHistoryManager;
import com.unifurniture.mobile.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private ProductCardAdapter recommendedAdapter;
    private final ScrollStateHelper scrollState = new ScrollStateHelper("home");
    private ApiService apiService;

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
        viewModel = NavViewModelProvider.get(this, R.id.homeFragment, HomeViewModel.class);
        apiService = UniFurnitureApp.getInstance().getApiService();
        scrollState.read(savedInstanceState);

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
                if (hasFocus) {
                    binding.layoutContent.setAlpha(0.3f); // Làm mờ nội dung chính
                    if (binding.searchView.getQuery().toString().trim().isEmpty()) {
                        showSearchHistory();
                    }
                } else {
                    binding.layoutContent.setAlpha(1.0f); // Hiện lại nội dung chính
                    hideSearchHistory();
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
                    binding.layoutContent.setAlpha(0.3f);
                    binding.layoutContent.setVisibility(View.VISIBLE);
                    return true;
                }
                binding.layoutContent.setAlpha(0.1f); // Mờ hẳn khi đang gõ để tập trung vào gợi ý
                hideSearchHistory();
                searchRunnable = () -> viewModel.searchForSuggestions(query);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshData();
            binding.swipeRefresh.setRefreshing(false);
        });

        scrollState.restore(binding.homeScrollView);

        syncBirthdayNotifications();
    }

    private void syncBirthdayNotifications() {
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        if (!sessionManager.isLoggedIn()) {
            return;
        }

        String customerId = sessionManager.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            return;
        }

        apiService.getNotifications(customerId, 20).enqueue(new Callback<ApiListResponse<NotificationDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiListResponse<NotificationDto>> call,
                                   @NonNull Response<ApiListResponse<NotificationDto>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null || response.body().items == null) {
                    return;
                }

                NotificationManager manager = NotificationManager.getInstance(requireContext());
                manager.syncRemoteNotifications(response.body().items);

                for (NotificationDto item : manager.getNotifications()) {
                    if (item == null) continue;
                    if (!"birthday_reward".equals(item.eventKey) || item.isRead) continue;
                    showBirthdayRewardDialog(item, manager);
                    break;
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiListResponse<NotificationDto>> call, @NonNull Throwable t) {
            }
        });
    }

    private void showBirthdayRewardDialog(NotificationDto item, NotificationManager manager) {
        if (!isAdded()) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_birthday_reward);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView titleView = dialog.findViewById(R.id.tvBirthdayTitle);
        TextView messageView = dialog.findViewById(R.id.tvBirthdayMessage);
        TextView codeView = dialog.findViewById(R.id.tvCouponCode);
        TextView subtleView = dialog.findViewById(R.id.tvBirthdaySubtle);
        com.google.android.material.button.MaterialButton closeButton =
                dialog.findViewById(R.id.btnCloseBirthday);
        com.google.android.material.button.MaterialButton copyButton =
                dialog.findViewById(R.id.btnCopyBirthdayCode);

        titleView.setText(R.string.birthday_popup_title);
        messageView.setText(R.string.birthday_popup_message);
        subtleView.setText(getString(R.string.birthday_reward_subtle));
        closeButton.setText(R.string.close);
        copyButton.setText(R.string.copy_code);

        String couponCode = item.couponCode == null ? "" : item.couponCode.trim();
        if (couponCode.isEmpty()) {
            codeView.setVisibility(View.GONE);
            copyButton.setVisibility(View.GONE);
        } else {
            codeView.setVisibility(View.VISIBLE);
            codeView.setText(couponCode);
            copyButton.setVisibility(View.VISIBLE);
            copyButton.setOnClickListener(v -> {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("coupon_code", couponCode));
                    android.widget.Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), android.widget.Toast.LENGTH_SHORT).show();
                }
                manager.markAsRead(item.id);
                dialog.dismiss();
            });
        }

        closeButton.setOnClickListener(v -> {
            manager.markAsRead(item.id);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> manager.markAsRead(item.id));
        dialog.show();
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
        String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
        // Featured products - horizontal scroll
        featuredAdapter = new ProductCardAdapter(serverHost, product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        featuredAdapter.setColumns(0); // carousel mode — don't force MATCH_PARENT width
        binding.rvFeaturedProducts.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeaturedProducts.setHasFixedSize(true);
        binding.rvFeaturedProducts.setAdapter(featuredAdapter);

        // "You may be interested" — same card style as featured, horizontal carousel
        recommendedAdapter = new ProductCardAdapter(serverHost, product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        recommendedAdapter.setColumns(0);
        binding.rvRecommended.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecommended.setHasFixedSize(true);
        binding.rvRecommended.setAdapter(recommendedAdapter);

        // Categories - 3 columns for readable labels and larger circular thumbnails
        categoryAdapter = new CategoryAdapter(serverHost, category -> {
            Bundle args = new Bundle();
            args.putString("categoryId", category.id);
            args.putString("categoryName", category.name);
            Navigation.findNavController(requireView()).navigate(R.id.productListFragment, args);
        });
        binding.rvCategories.setLayoutManager(
                new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3));
        binding.rvCategories.setAdapter(categoryAdapter);
        binding.rvCategories.setNestedScrollingEnabled(false); // Smooth scroll in NestedScrollView

        // Promotions coupon carousel
        couponHomeAdapter = new CouponHomeAdapter();
        if (binding.rvPromotionCoupons != null) {
            binding.rvPromotionCoupons.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.rvPromotionCoupons.setHasFixedSize(true);
            binding.rvPromotionCoupons.setAdapter(couponHomeAdapter);
        }

        if (binding.btnViewAllPromotions != null) {
            binding.btnViewAllPromotions.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("entry_mode", "browse");
                bundle.putDouble("subtotal", 0.0);
                Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment, bundle);
            });
        }

        // Collections
        collectionAdapter = new CollectionAdapter(serverHost, collection -> {
            Bundle args = new Bundle();
            args.putString("collectionId", collection.id);
            Navigation.findNavController(requireView()).navigate(R.id.productListFragment, args);
        });
        binding.rvCollections.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCollections.setHasFixedSize(true);
        binding.rvCollections.setAdapter(collectionAdapter);
    }

    private void observeData() {
        viewModel.getFeaturedProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                featuredAdapter.submitList(response.items);
            } else if (response == null
                    && !com.unifurniture.mobile.util.NetworkUtil.isOnline(requireContext())) {
                // Load failed while offline → offer a retry.
                com.unifurniture.mobile.util.CustomBlueDialog.showNoInternet(
                        requireContext(), () -> viewModel.refreshData());
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                int previewCount = Math.min(items.size(), 9);
                categoryAdapter.submitList(items.subList(0, previewCount));
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

        viewModel.getRecommended().observe(getViewLifecycleOwner(), products -> {
            if (binding == null) return;
            if (products != null && !products.isEmpty()) {
                recommendedAdapter.submitList(products);
                binding.layoutRecommended.setVisibility(View.VISIBLE);
            } else {
                binding.layoutRecommended.setVisibility(View.GONE);
            }
        });

        viewModel.getSearchSuggestions().observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                searchSuggestionAdapter.submitList(products);
                binding.rvSearchSuggestions.setVisibility(View.VISIBLE);
                binding.layoutContent.setVisibility(View.GONE);
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
        binding.layoutContent.setVisibility(View.VISIBLE);
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
        binding.rvRecentlyViewed.setHasFixedSize(true);
        binding.rvRecentlyViewed.setAdapter(recentlyViewedAdapter);
    }

    private void refreshRecentlyViewed() {
        // Refresh the "You may be interested" row from the latest view/purchase history.
        if (viewModel != null) viewModel.loadRecommended();

        List<RecentlyViewedManager.Item> items = recentlyViewedManager.getAll();
        if (items.isEmpty()) {
            binding.layoutRecentlyViewed.setVisibility(View.GONE);
        } else {
            recentlyViewedAdapter.submitList(items);
            binding.layoutRecentlyViewed.setVisibility(View.VISIBLE);
            // If any names were cached in another language, refetch them in the current one.
            if (viewModel != null) {
                viewModel.refreshRecentlyViewedIfStale(() -> {
                    if (binding != null) recentlyViewedAdapter.submitList(recentlyViewedManager.getAll());
                });
            }
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
        // Re-fetch home data if the UI language changed while we were away, so categories and
        // products come back in the newly selected language instead of the cached one.
        if (viewModel != null) viewModel.reloadIfLanguageChanged();
        if (binding != null) {
            binding.getRoot().post(this::clearSearchUiState);
        }
        if (bannerAdapter != null && bannerAdapter.getItemCount() > 1) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
        }
        if (recentlyViewedManager != null) refreshRecentlyViewed();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null) scrollState.saveScroll(binding.homeScrollView);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (autoScrollHandler != null) autoScrollHandler.removeCallbacks(autoScrollRunnable);
        clearSearchUiState();
    }

    private void clearSearchUiState() {
        if (binding == null) return;
        binding.searchView.clearFocus();
        hideSearchSuggestions();
        hideSearchHistory();
        binding.layoutContent.setAlpha(1.0f);

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchView.getWindowToken(), 0);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            scrollState.save(outState, binding.homeScrollView);
        }
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
