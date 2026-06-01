package com.unifurniture.mobile.ui.product;

import android.content.Context;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.databinding.FragmentProductListBinding;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import com.unifurniture.mobile.ui.adapter.SearchHistoryAdapter;
import com.unifurniture.mobile.ui.adapter.SearchSuggestionAdapter;
import com.unifurniture.mobile.util.SearchHistoryManager;
import java.util.ArrayList;
import java.util.List;

public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductCardAdapter adapter;
    private boolean isGrid = true;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private TextWatcher searchWatcher;
    private SearchSuggestionAdapter suggestionAdapter;
    private SearchHistoryManager historyManager;
    private SearchHistoryAdapter historyAdapter;

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
        viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);

        historyManager = new SearchHistoryManager(requireContext());
        setupRecyclerView();
        handleArguments();  // populate etSearch BEFORE watcher is attached
        setupSearch();
        observeData();
        syncSortChip();

        // Sort chips
        binding.chipNewest.setOnClickListener(v -> viewModel.sortBy("createdAt", "desc"));
        binding.chipPriceLow.setOnClickListener(v -> viewModel.sortBy("min_price", "asc"));
        binding.chipPriceHigh.setOnClickListener(v -> viewModel.sortBy("min_price", "desc"));
        binding.chipBestSelling.setOnClickListener(v -> viewModel.sortBy("sold", "desc"));


        binding.fabFilter.setOnClickListener(v -> showFilterSheet());
        binding.btnToggleLayout.setOnClickListener(v -> toggleLayout());

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadProducts();
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
                viewModel.search(search);
                historyManager.add(search);
                filterApplied = true;
            }
            if (categoryId != null) {
                viewModel.filterByCategory(categoryId);
                if (categoryName != null) binding.tvTitle.setText(categoryName);
                filterApplied = true;
            }
            if (collectionId != null) {
                viewModel.filterByCollection(collectionId);
                filterApplied = true;
            }
        }
        if (!filterApplied) viewModel.loadProducts();
    }

    private void setupRecyclerView() {
        adapter = new ProductCardAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(adapter);
        binding.rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= lm.getItemCount() - 3) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void toggleLayout() {
        isGrid = !isGrid;
        int span = isGrid ? 2 : 1;
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), span));
        adapter.setColumns(span);
        binding.btnToggleLayout.setImageResource(
                isGrid ? R.drawable.ic_view_list : R.drawable.ic_grid_view);
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
                    binding.progressBar.setVisibility(View.GONE);
                    searchRunnable = () -> { viewModel.search(""); syncSortChip(); };
                    searchHandler.postDelayed(searchRunnable, 300);
                    return;
                }

                hideSearchHistory();
                binding.progressBar.setVisibility(View.VISIBLE);
                showClientSideSuggestions(query);

                searchRunnable = () -> { viewModel.search(query); syncSortChip(); };
                searchHandler.postDelayed(searchRunnable, 300);
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

    private void showClientSideSuggestions(String query) {
        ApiListResponse<ProductDto> current = viewModel.getProducts().getValue();
        if (current == null || current.items == null) return;
        String normalizedQuery = stripDiacritics(query.toLowerCase());
        List<ProductDto> matched = new ArrayList<>();
        for (ProductDto p : current.items) {
            if (p.name != null && matchesWordStart(stripDiacritics(p.name.toLowerCase()), normalizedQuery)) {
                matched.add(p);
                if (matched.size() >= 8) break;
            }
        }
        if (!matched.isEmpty()) {
            suggestionAdapter.submitList(matched);
            binding.rvSuggestions.setVisibility(View.VISIBLE);
        } else {
            hideSuggestions();
        }
    }

    private String stripDiacritics(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    // Trả về true nếu query khớp với đầu tên hoặc đầu một từ trong tên
    private boolean matchesWordStart(String normalizedName, String normalizedQuery) {
        return normalizedName.startsWith(normalizedQuery);
    }

    private void hideSuggestions() {
        binding.rvSuggestions.setVisibility(View.GONE);
    }

    private void observeData() {
        viewModel.getProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                adapter.submitList(response.items);
                binding.tvProductCount.setText("Hiển thị " + response.items.size() + " / " + response.total + " sản phẩm");
                boolean empty = response.items.isEmpty();
                binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);

                // Refresh suggestions with API results
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty() && !response.items.isEmpty()) {
                    int limit = Math.min(8, response.items.size());
                    suggestionAdapter.submitList(response.items.subList(0, limit));
                    binding.rvSuggestions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.isLoadingMore().observe(getViewLifecycleOwner(), more ->
                binding.progressBarLoadMore.setVisibility(more ? View.VISIBLE : View.GONE));
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
            }

            @Override
            public void onFiltersCleared() {
                binding.etSearch.removeTextChangedListener(searchWatcher);
                binding.etSearch.setText("");
                binding.etSearch.addTextChangedListener(searchWatcher);
                hideSuggestions();
                viewModel.clearFilters();
                syncSortChip();
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
}
