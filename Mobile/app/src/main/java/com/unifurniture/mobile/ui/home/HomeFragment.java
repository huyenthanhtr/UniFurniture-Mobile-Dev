package com.unifurniture.mobile.ui.home;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentHomeBinding;
import com.unifurniture.mobile.ui.adapter.CategoryAdapter;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ProductCardAdapter featuredAdapter;
    private CategoryAdapter categoryAdapter;

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
        observeData();

        binding.btnViewAll.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.productListFragment));

        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Bundle args = new Bundle();
                args.putString("search", query);
                Navigation.findNavController(requireView()).navigate(R.id.productListFragment, args);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadData();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupRecyclerViews() {
        // Featured products - horizontal scroll
        featuredAdapter = new ProductCardAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
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
    }

    private void observeData() {
        viewModel.getFeaturedProducts().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                featuredAdapter.submitList(response.items);
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                categoryAdapter.submitList(response.items);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.shimmerLayout.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
