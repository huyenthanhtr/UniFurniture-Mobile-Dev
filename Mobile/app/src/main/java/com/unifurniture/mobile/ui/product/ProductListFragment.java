package com.unifurniture.mobile.ui.product;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.Normalizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.res.ColorStateList;
import com.google.android.material.chip.Chip;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.databinding.FragmentProductListBinding;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import com.unifurniture.mobile.ui.adapter.SearchHistoryAdapter;
import com.unifurniture.mobile.ui.adapter.SearchSuggestionAdapter;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;
import com.unifurniture.mobile.util.SearchHistoryManager;
import java.util.ArrayList;
import java.util.List;

public class ProductListFragment extends Fragment {

    private static final String KEY_IS_GRID = "is_grid";
    private static final String PREF_PRODUCTS_SCROLL = "products_scroll_state";
    private static final String PREF_KEY_REQUEST = "request_key";
    private static final String PREF_KEY_POSITION = "position";
    private static final String PREF_KEY_OFFSET = "offset";

    private FragmentProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductCardAdapter adapter;
    private boolean isGrid = true;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("products");
    private Handler searchHandler;
    private Runnable searchRunnable;
    private TextWatcher searchWatcher;
    private SearchSuggestionAdapter suggestionAdapter;
    private SearchHistoryManager historyManager;
    private SearchHistoryAdapter historyAdapter;
    private List<ProductDto> fullProductList = new ArrayList<>(); // Bản sao để lọc tức thì
    private boolean pendingScrollRestore = false;
    private boolean restoringScroll = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.productListFragment, ProductListViewModel.class);

        if (savedInstanceState != null) {
            isGrid = savedInstanceState.getBoolean(KEY_IS_GRID, true);
        }

        historyManager = new SearchHistoryManager(requireContext());
        setupRecyclerView();
        rvState.bind(binding.rvProducts, savedInstanceState);
        handleArguments();  // populate etSearch BEFORE watcher is attached
        restorePersistedScrollState();
        pendingScrollRestore = savedInstanceState != null || viewModel.hasSavedScrollState();
        setupSearch();
        observeData();
        styleChips();
        syncSortChip();

        // Sort chips
        binding.chipNewest.setOnClickListener(v -> { viewModel.sortBy("createdAt", "desc"); syncSortChip(); });
        binding.chipPriceLow.setOnClickListener(v -> { viewModel.sortBy("min_price", "asc"); syncSortChip(); });
        binding.chipPriceHigh.setOnClickListener(v -> { viewModel.sortBy("min_price", "desc"); syncSortChip(); });
        binding.chipBestSelling.setOnClickListener(v -> { viewModel.sortBy("sold", "desc"); syncSortChip(); });


        binding.btnLoadMore.setOnClickListener(v -> viewModel.loadNextPage());

        // Show Load More only when scrolled near the bottom
        binding.rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!restoringScroll && (dx != 0 || dy != 0)) {
                    pendingScrollRestore = false;
                    saveCurrentScrollState();
                }

                GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                int total = lm.getItemCount();
                boolean nearEnd = total > 0 && lastVisible >= total - 4;
                boolean loadingMore = Boolean.TRUE.equals(viewModel.isLoadingMore().getValue());
                if (nearEnd && viewModel.hasMorePages() && !loadingMore) {
                    binding.btnLoadMore.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }
        });

        binding.fabFilter.setOnClickListener(v -> showFilterSheet());
        binding.btnToggleLayout.setOnClickListener(v -> toggleLayout());

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshProducts();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void handleArguments() {
        Bundle args = getArguments();
        boolean filterApplied = false;
        if (args != null) {
            String search = args.getString("search");
            String categoryId = args.getString("categoryId");
            String categoryName = args.getString("categoryName");
            String collectionId = args.getString("collectionId");

            if (search != null) {
                binding.etSearch.setText(search);
                historyManager.add(search);
                filterApplied = true;
            }
            if (categoryId != null) {
                if (categoryName != null) binding.tvTitle.setText(categoryName);
                filterApplied = true;
            }
            if (collectionId != null) {
                filterApplied = true;
            }
            if (filterApplied && !viewModel.matchesCurrentRequest(search, categoryId, collectionId)) {
                viewModel.applyRequest(search, categoryId, collectionId);
            }
        }
        if (!filterApplied && !viewModel.hasLoadedProducts()) {
            viewModel.loadProductsIfNeeded();
        }
        updateFilterBadge();
        updateActiveFilterChips();
    }

    private void setupRecyclerView() {
        String serverHost = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");
        adapter = new ProductCardAdapter(serverHost, product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        int span = isGrid ? 2 : 1;
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), span));
        adapter.setColumns(span);
        binding.rvProducts.setAdapter(adapter);
        binding.btnToggleLayout.setImageResource(
                isGrid ? R.drawable.ic_view_list : R.drawable.ic_grid_view);
    }

    private void restoreRecyclerViewState() {
        rvState.restoreIfPending();
        restoreSavedScrollState();
    }

    private void maybeRestoreRecyclerViewState() {
        restoreRecyclerViewState();
    }

    private void saveCurrentScrollState() {
        if (binding == null || restoringScroll) return;
        RecyclerView.LayoutManager manager = binding.rvProducts.getLayoutManager();
        if (!(manager instanceof GridLayoutManager layoutManager)) return;

        int position = layoutManager.findFirstVisibleItemPosition();
        if (position == RecyclerView.NO_POSITION) return;

        View firstChild = layoutManager.findViewByPosition(position);
        int offset = firstChild != null
                ? firstChild.getTop() - binding.rvProducts.getPaddingTop()
                : 0;
        viewModel.saveScrollState(position, offset);
        persistScrollState(position, offset);
    }

    private void persistScrollState(int position, int offset) {
        if (viewModel == null) return;
        requireContext().getSharedPreferences(PREF_PRODUCTS_SCROLL, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_REQUEST, viewModel.getCurrentScrollKey())
                .putInt(PREF_KEY_POSITION, position)
                .putInt(PREF_KEY_OFFSET, offset)
                .apply();
    }

    private void restorePersistedScrollState() {
        if (viewModel == null) return;
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_PRODUCTS_SCROLL, Context.MODE_PRIVATE);
        String savedRequest = prefs.getString(PREF_KEY_REQUEST, null);
        if (!viewModel.getCurrentScrollKey().equals(savedRequest)) return;

        int position = prefs.getInt(PREF_KEY_POSITION, RecyclerView.NO_POSITION);
        if (position == RecyclerView.NO_POSITION) return;

        int offset = prefs.getInt(PREF_KEY_OFFSET, 0);
        viewModel.saveScrollState(position, offset);
    }

    private void restoreSavedScrollState() {
        if (!pendingScrollRestore || binding == null || !viewModel.hasSavedScrollState()) return;
        RecyclerView.LayoutManager manager = binding.rvProducts.getLayoutManager();
        if (!(manager instanceof GridLayoutManager layoutManager)) return;
        if (adapter.getItemCount() <= viewModel.getSavedScrollPosition()) return;

        binding.rvProducts.post(() -> {
            if (binding == null || !pendingScrollRestore || !viewModel.hasSavedScrollState()) return;
            restoringScroll = true;
            binding.rvProducts.stopScroll();
            layoutManager.scrollToPositionWithOffset(
                    viewModel.getSavedScrollPosition(),
                    viewModel.getSavedScrollOffset());
            binding.rvProducts.post(() -> {
                pendingScrollRestore = false;
                restoringScroll = false;
            });
        });
    }

    private void toggleLayout() {
        isGrid = !isGrid;
        int span = isGrid ? 2 : 1;
        GridLayoutManager layoutManager = (GridLayoutManager) binding.rvProducts.getLayoutManager();
        if (layoutManager == null) {
            layoutManager = new GridLayoutManager(requireContext(), span);
            binding.rvProducts.setLayoutManager(layoutManager);
        } else {
            layoutManager.setSpanCount(span);
        }
        adapter.setColumns(span);
        binding.btnToggleLayout.setImageResource(
                isGrid ? R.drawable.ic_view_list : R.drawable.ic_grid_view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_GRID, isGrid);
        rvState.save(outState);
    }

    private void setupSearch() {
        // Suggestions
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestionAdapter = new SearchSuggestionAdapter(product -> {
            hideSuggestions();
            hideSearchHistory();
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
            String slug = product.slug != null ? product.slug : product.id;
            Bundle args = new Bundle();
            args.putString("slug", slug);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        binding.rvSuggestions.setAdapter(suggestionAdapter);

        // History
        historyAdapter = new SearchHistoryAdapter(new SearchHistoryAdapter.OnItemListener() {
            @Override
            public void onQueryClick(String query) {
                hideSearchHistory();
                binding.etSearch.removeTextChangedListener(searchWatcher);
                binding.etSearch.setText(query);
                binding.etSearch.setSelection(query.length());
                binding.etSearch.addTextChangedListener(searchWatcher);
                historyManager.add(query);
                viewModel.search(query);
                syncSortChip();
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
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

        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && binding.etSearch.getText().toString().trim().isEmpty()) {
                showSearchHistory();
            } else if (!hasFocus) {
                hideSearchHistory();
                // Do NOT hideSuggestions() here — focus loss fires before suggestion
                // click completes, causing the tap to miss. Suggestions hide via
                // onTextChanged (empty) or suggestion tap handler.
            }
        });

        // Save to history on IME submit
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = binding.etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                historyManager.add(query);
                hideSuggestions();
                hideSearchHistory();
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
            }
            return false;
        });

        searchHandler = new Handler(Looper.getMainLooper());
        searchWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchHandler != null && searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    hideSuggestions();
                    showSearchHistory();
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.GONE);
                    // Khi xóa trắng, hiện lại toàn bộ danh sách gốc
                    adapter.submitList(new ArrayList<>(fullProductList));
                    searchRunnable = () -> { viewModel.search(""); syncSortChip(); };
                    searchHandler.postDelayed(searchRunnable, 300);
                    return;
                }

                hideSearchHistory();
                binding.layoutEmpty.setVisibility(View.GONE); // Ẩn ngay lập tức để không gây hiểu lầm
                
                // Lọc "mềm" tức thì trên dữ liệu đang có
                performInstantLocalFilter(query);

                searchRunnable = () -> { viewModel.search(query); syncSortChip(); };
                searchHandler.postDelayed(searchRunnable, 600); // Đợi khách gõ xong mới gọi Server
            }
        };
        binding.etSearch.addTextChangedListener(searchWatcher);
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

    private List<ProductDto> getLocalMatches(String query) {
        String normalizedQuery = FormatUtil.stripDiacritics(query.toLowerCase());
        List<ProductDto> matched = new ArrayList<>();
        for (ProductDto p : fullProductList) {
            if (p.name != null) {
                String normalizedName = FormatUtil.stripDiacritics(p.name.toLowerCase());
                if (normalizedName.contains(normalizedQuery)) {
                    matched.add(p);
                }
            }
        }
        return sortByRelevance(matched, query);
    }

    private void performInstantLocalFilter(String query) {
        List<ProductDto> matched = getLocalMatches(query);
        adapter.submitList(new ArrayList<>(matched));
        
        if (!matched.isEmpty()) {
            int limit = Math.min(8, matched.size());
            suggestionAdapter.submitList(new ArrayList<>(matched.subList(0, limit)));
            binding.rvSuggestions.setVisibility(View.VISIBLE);
        } else {
            hideSuggestions();
        }
    }

    // Trả về true nếu tên sản phẩm chứa từ khóa (không phân biệt hoa thường, dấu)
    private boolean matchesWordStart(String normalizedName, String normalizedQuery) {
        return normalizedName.contains(normalizedQuery);
    }

    private void hideSuggestions() {
        binding.rvSuggestions.setVisibility(View.GONE);
    }

    private void observeData() {
        viewModel.getProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                String query = binding.etSearch.getText().toString().trim();
                
                // 1. Cập nhật bản sao dữ liệu khi không ở chế độ tìm kiếm hoặc khi Server có hàng
                if (query.isEmpty() || !response.items.isEmpty()) {
                    fullProductList = new ArrayList<>(response.items);
                }
                
                List<ProductDto> displayList = response.items;
                boolean loading = Boolean.TRUE.equals(viewModel.isLoading().getValue());

                // Sắp xếp lại theo độ liên quan nếu đang tìm kiếm
                if (!query.isEmpty()) {
                    // UX FIX: Nếu Server không thấy kết quả nhưng bộ lọc máy thấy, ưu tiên dùng máy
                    if (displayList.isEmpty()) {
                        List<ProductDto> localMatches = getLocalMatches(query);
                        if (!localMatches.isEmpty()) {
                            displayList = localMatches;
                        }
                    } else {
                        // Luôn sắp xếp lại kết quả từ Server theo độ liên quan
                        displayList = sortByRelevance(displayList, query);
                    }
                }

                adapter.submitList(new ArrayList<>(displayList), this::maybeRestoreRecyclerViewState);
                binding.tvProductCount.setText(getString(R.string.showing_products, displayList.size(), response.total));
                
                // 2. Chỉ hiện thông báo "Không tìm thấy" khi cả Server và Máy đều rỗng
                boolean empty = displayList.isEmpty() && !loading && !query.isEmpty();
                
                binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.btnLoadMore.setVisibility(View.GONE);
                
                // 3. Cập nhật gợi ý dựa trên danh sách đang hiển thị
                if (!query.isEmpty() && !displayList.isEmpty()) {
                    int limit = Math.min(8, displayList.size());
                    suggestionAdapter.submitList(new ArrayList<>(displayList.subList(0, limit)));
                    binding.rvSuggestions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isInitialLoad = loading && (adapter.getItemCount() == 0 || binding.rvProducts.getVisibility() == View.GONE);
            if (loading) {
                binding.layoutEmpty.setVisibility(View.GONE); // Luôn ẩn "Trống" khi đang loading
            }
            if (isInitialLoad) {
                binding.shimmerLayout.setVisibility(View.VISIBLE);
                binding.shimmerLayout.startShimmer();
                binding.swipeRefresh.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
            } else {
                binding.shimmerLayout.stopShimmer();
                binding.shimmerLayout.setVisibility(View.GONE);
                binding.swipeRefresh.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.isLoadingMore().observe(getViewLifecycleOwner(), more -> {
            binding.progressBarLoadMore.setVisibility(more ? View.VISIBLE : View.GONE);
            binding.btnLoadMore.setVisibility(
                    !more && viewModel.hasMorePages() ? View.VISIBLE : View.GONE);
        });
    }

    private void updateFilterBadge() {
        int count = 0;
        if (viewModel.getCurrentCategoryId() != null) count++;
        if (viewModel.getCurrentMinPrice() != null || viewModel.getCurrentMaxPrice() != null) count++;
        if (viewModel.getCurrentMinRating() > 0) count++;
        // Thay badge đỏ bằng đổi màu FAB: xanh = không lọc, vàng gold = đang lọc
        int tint = count > 0
                ? requireContext().getColor(R.color.accent)
                : requireContext().getColor(R.color.primary);
        binding.fabFilter.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(tint));
        binding.tvFilterBadge.setVisibility(View.GONE);
    }

    private void updateActiveFilterChips() {
        binding.chipGroupActiveFilters.removeAllViews();
        boolean hasActive = false;

        String catId = viewModel.getCurrentCategoryId();
        if (catId != null) {
            String catName = catId;
            List<CategoryDto> cats = viewModel.getCategories().getValue();
            if (cats != null) {
                for (CategoryDto c : cats) {
                    if (catId.equals(c.id)) { catName = c.name; break; }
                }
            }
            final String finalCatName = catName;
            addActiveChip(finalCatName, () -> {
                viewModel.applyFilters(null,
                        viewModel.getCurrentMinPrice(),
                        viewModel.getCurrentMaxPrice(),
                        viewModel.getCurrentMinRating());
                updateFilterBadge();
                updateActiveFilterChips();
            });
            hasActive = true;
        }

        Double minP = viewModel.getCurrentMinPrice();
        Double maxP = viewModel.getCurrentMaxPrice();
        if (minP != null || maxP != null) {
            String label = FormatUtil.formatCurrency(minP) + " – " + FormatUtil.formatCurrency(maxP);
            addActiveChip(label, () -> {
                viewModel.applyFilters(viewModel.getCurrentCategoryId(), null, null,
                        viewModel.getCurrentMinRating());
                updateFilterBadge();
                updateActiveFilterChips();
            });
            hasActive = true;
        }

        int minRating = viewModel.getCurrentMinRating();
        if (minRating > 0) {
            addActiveChip(minRating + "★+", () -> {
                viewModel.applyFilters(viewModel.getCurrentCategoryId(),
                        viewModel.getCurrentMinPrice(),
                        viewModel.getCurrentMaxPrice(), 0);
                updateFilterBadge();
                updateActiveFilterChips();
            });
            hasActive = true;
        }

        binding.scrollActiveFilters.setVisibility(hasActive ? View.VISIBLE : View.GONE);
    }

    private void addActiveChip(String label, Runnable onRemove) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> onRemove.run());
        binding.chipGroupActiveFilters.addView(chip);
    }

    private void styleChips() {
        int colorAccent  = requireContext().getColor(R.color.accent);
        int colorPrimary = requireContext().getColor(R.color.primary);
        int colorGray200 = requireContext().getColor(R.color.gray_200);
        int colorWhite   = requireContext().getColor(R.color.white);
        int colorBlack   = requireContext().getColor(R.color.black);

        ColorStateList bg = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorAccent, colorGray200});
        ColorStateList text = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorPrimary, colorBlack});

        for (int id : new int[]{R.id.chipNewest, R.id.chipBestSelling, R.id.chipPriceLow, R.id.chipPriceHigh}) {
            Chip chip = binding.getRoot().findViewById(id);
            if (chip == null) continue;
            chip.setChipBackgroundColor(bg);
            chip.setTextColor(text);
            chip.setCheckedIconVisible(false);
            chip.setChipStrokeWidth(0f);
        }
    }

    private void syncSortChip() {
        String s = viewModel.getCurrentSortBy();
        String o = viewModel.getCurrentOrder();
        binding.chipNewest.setChecked("createdAt".equals(s));
        binding.chipBestSelling.setChecked("sold".equals(s));
        binding.chipPriceLow.setChecked("min_price".equals(s) && "asc".equals(o));
        binding.chipPriceHigh.setChecked("min_price".equals(s) && "desc".equals(o));
    }

    private void showFilterSheet() {
        List<CategoryDto> cats = viewModel.getCategories().getValue();
        ArrayList<String> catIds = new ArrayList<>();
        ArrayList<String> catNames = new ArrayList<>();
        if (cats != null) {
            for (CategoryDto c : cats) {
                if (c.id != null && c.name != null) {
                    catIds.add(c.id);
                    catNames.add(c.name);
                }
            }
        }

        FilterBottomSheetFragment sheet = FilterBottomSheetFragment.newInstance(
                catIds, catNames,
                viewModel.getCurrentCategoryId(),
                viewModel.getCurrentMinPrice(),
                viewModel.getCurrentMaxPrice(),
                viewModel.getCurrentMinRating());

        sheet.setOnFiltersAppliedListener(new FilterBottomSheetFragment.OnFiltersAppliedListener() {
            @Override
            public void onFiltersApplied(String categoryId, Double minPrice, Double maxPrice, int minRating) {
                viewModel.applyFilters(categoryId, minPrice, maxPrice, minRating);
                updateFilterBadge();
                updateActiveFilterChips();
            }

            @Override
            public void onFiltersCleared() {
                binding.etSearch.removeTextChangedListener(searchWatcher);
                binding.etSearch.setText("");
                binding.etSearch.addTextChangedListener(searchWatcher);
                hideSuggestions();
                viewModel.clearFilters();
                syncSortChip();
                updateFilterBadge();
                updateActiveFilterChips();
            }
        });

        sheet.show(getChildFragmentManager(), "filter");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        binding = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only restore scroll here when onViewCreated did NOT already set it up
        // (e.g. fragment was paused/resumed without view recreation).
        if (binding != null && viewModel != null && viewModel.hasSavedScrollState() && !pendingScrollRestore) {
            pendingScrollRestore = true;
            restoreSavedScrollState();
        }
    }
}
