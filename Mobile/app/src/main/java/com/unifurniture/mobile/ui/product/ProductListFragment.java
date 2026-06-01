package com.unifurniture.mobile.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.databinding.FragmentProductListBinding;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import java.util.ArrayList;
import java.util.List;

public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductCardAdapter adapter;
    private boolean isGrid = true;

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

        setupRecyclerView();
        handleArguments();
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
                viewModel.search(search);
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

    private void observeData() {
        viewModel.getProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                adapter.submitList(response.items);
                binding.tvProductCount.setText("Hiển thị " + response.items.size() + " / " + response.total + " sản phẩm");
                boolean empty = response.items.isEmpty();
                binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
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
                viewModel.clearFilters();
                syncSortChip();
            }
        });

        sheet.show(getChildFragmentManager(), "filter");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
