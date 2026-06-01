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
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentProductListBinding;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;

public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductCardAdapter adapter;

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

        // Sort chips
        binding.chipNewest.setOnClickListener(v -> viewModel.sortBy("createdAt", "desc"));
        binding.chipPriceLow.setOnClickListener(v -> viewModel.sortBy("min_price", "asc"));
        binding.chipPriceHigh.setOnClickListener(v -> viewModel.sortBy("min_price", "desc"));
        binding.chipBestSelling.setOnClickListener(v -> viewModel.sortBy("sold", "desc"));

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
    }

    private void observeData() {
        viewModel.getProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                adapter.submitList(response.items);
                binding.tvProductCount.setText(response.total + " sản phẩm");
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
