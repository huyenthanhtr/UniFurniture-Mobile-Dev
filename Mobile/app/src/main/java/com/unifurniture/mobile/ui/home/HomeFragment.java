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
import com.unifurniture.mobile.databinding.FragmentHomeBinding;
import com.unifurniture.mobile.ui.adapter.CategoryAdapter;
import com.unifurniture.mobile.ui.adapter.ImageSliderAdapter;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ProductCardAdapter featuredAdapter;
    private CategoryAdapter categoryAdapter;
    private ImageSliderAdapter bannerAdapter;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;

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

    private void setupBanner() {
        bannerAdapter = new ImageSliderAdapter(requireContext(), new ArrayList<>());
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

        viewModel.getCategories().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                categoryAdapter.submitList(items);
            }
        });

        viewModel.getCollections().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) return;
            String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
            List<String> bannerUrls = new ArrayList<>();
            for (CollectionDto c : items) {
                if (c.bannerUrl != null && !c.bannerUrl.isEmpty()) {
                    bannerUrls.add(c.bannerUrl.replace("http://localhost:3000", serverHost));
                }
            }
            if (bannerUrls.isEmpty()) return;
            bannerAdapter.updateImages(bannerUrls);
            binding.bannerDotsIndicator.setVisibility(
                    bannerUrls.size() > 1 ? View.VISIBLE : View.GONE);
            if (bannerUrls.size() > 1) {
                autoScrollHandler.removeCallbacks(autoScrollRunnable);
                autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.shimmerLayout.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerAdapter != null && bannerAdapter.getItemCount() > 1) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
        }
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
        binding = null;
    }
}
