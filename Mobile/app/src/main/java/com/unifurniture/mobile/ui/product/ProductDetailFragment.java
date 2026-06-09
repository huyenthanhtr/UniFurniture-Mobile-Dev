package com.unifurniture.mobile.ui.product;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.databinding.FragmentProductDetailBinding;
import com.unifurniture.mobile.ui.adapter.ImageSliderAdapter;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import com.unifurniture.mobile.ui.adapter.ReviewAdapter;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.RecentlyViewedManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailFragment extends Fragment {

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    private ImageSliderAdapter sliderAdapter;
    private ReviewAdapter reviewAdapter;
    private ProductCardAdapter recommendationAdapter;
    private RecentlyViewedManager recentlyViewedManager;
    private String selectedVariantId = null;
    private int quantity = 1;
    private final Map<String, Integer> variantStock = new HashMap<>();
    private final List<ReviewDto> allReviews = new ArrayList<>();
    private final Handler cartRestoreHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);

        String slug = getArguments() != null ? getArguments().getString("slug") : null;
        if (slug == null) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }

        recentlyViewedManager = new RecentlyViewedManager(requireContext());
        setupImageSlider();
        setupReviews();
        setupRecommendations();
        setupReviewSort();
        viewModel.loadProduct(slug);
        observeData();

        binding.btnDecrease.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.tvQuantity.setText(String.valueOf(quantity));
            }
        });
        binding.btnIncrease.setOnClickListener(v -> {
            if (quantity < 99) {
                quantity++;
                binding.tvQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.btnAddToCart.setOnClickListener(v -> viewModel.addToCart(selectedVariantId, quantity));
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
    }

    private void setupImageSlider() {
        sliderAdapter = new ImageSliderAdapter(requireContext(), new ArrayList<>());
        binding.viewPagerImages.setAdapter(sliderAdapter);
        binding.dotsIndicator.attachTo(binding.viewPagerImages);
    }

    private void setupReviews() {
        String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
        reviewAdapter = new ReviewAdapter(serverHost);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(reviewAdapter);
    }

    private void setupRecommendations() {
        recommendationAdapter = new ProductCardAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("slug", product.slug != null ? product.slug : product.id);
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        binding.rvRecommendations.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecommendations.setAdapter(recommendationAdapter);
    }

    private void setupReviewSort() {
        binding.chipSortNewest.setOnClickListener(v -> sortReviews("newest"));
        binding.chipSortHighRating.setOnClickListener(v -> sortReviews("high"));
        binding.chipSortLowRating.setOnClickListener(v -> sortReviews("low"));
    }

    private void sortReviews(String mode) {
        if (allReviews.isEmpty()) return;
        List<ReviewDto> sorted = new ArrayList<>(allReviews);
        switch (mode) {
            case "high":
                Collections.sort(sorted, (a, b) -> {
                    int ra = a.rating != null ? a.rating : 0;
                    int rb = b.rating != null ? b.rating : 0;
                    return rb - ra;
                });
                break;
            case "low":
                Collections.sort(sorted, (a, b) -> {
                    int ra = a.rating != null ? a.rating : 0;
                    int rb = b.rating != null ? b.rating : 0;
                    return ra - rb;
                });
                break;
            default: // newest
                Collections.sort(sorted, (a, b) -> {
                    if (b.createdAt == null) return -1;
                    if (a.createdAt == null) return 1;
                    return b.createdAt.compareTo(a.createdAt);
                });
                break;
        }
        reviewAdapter.submitList(sorted);
    }

    private void observeData() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getProduct().observe(getViewLifecycleOwner(), product -> {
            if (product == null) return;
            binding.tvProductName.setText(product.name);
            binding.tvPrice.setText(FormatUtil.formatCurrency(product.minPrice));
            if (product.compareAtPrice != null && product.compareAtPrice > 0
                    && (product.minPrice == null || product.compareAtPrice > product.minPrice)) {
                binding.tvOriginalPrice.setText(FormatUtil.formatCurrency(product.compareAtPrice));
                binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                String badge = FormatUtil.discountBadge(product.minPrice, product.compareAtPrice);
                if (badge != null) {
                    binding.tvDiscount.setText(badge);
                    binding.tvDiscount.setVisibility(View.VISIBLE);
                }
            }
            binding.tvDescription.setText(product.description);
            binding.tvShortDesc.setText(product.shortDescription);
            if (product.warrantyMonths != null && product.warrantyMonths > 0) {
                binding.tvWarranty.setText(getString(R.string.str_warranty_format, product.warrantyMonths));
                binding.tvWarranty.setVisibility(View.VISIBLE);
            }
            int soldCount = product.sold != null ? product.sold : 0;
            binding.tvSoldCount.setText(getString(R.string.sold_count, FormatUtil.formatSold(soldCount)));
            binding.tvSoldCount.setVisibility(View.VISIBLE);
            // Save to recently viewed + use thumbnail for slider fallback
            String thumb = product.getImageUrl();
            String slugKey = product.slug != null ? product.slug : product.id;
            recentlyViewedManager.add(new RecentlyViewedManager.Item(
                    product.id, slugKey, product.name, thumb));
            if (sliderAdapter.getItemCount() == 0 && thumb != null && !thumb.isEmpty()) {
                List<String> fallback = new ArrayList<>();
                fallback.add(thumb);
                sliderAdapter.updateImages(fallback);
            }
        });

        viewModel.getImages().observe(getViewLifecycleOwner(), response -> {
            List<String> urls = new ArrayList<>();
            if (response != null && response.items != null) {
                for (var img : response.items) {
                    if (img.imageUrl != null && !img.imageUrl.isEmpty()) urls.add(img.imageUrl);
                }
            }
            if (urls.isEmpty()) {
                ProductDto p = viewModel.getProduct().getValue();
                String thumb = p != null ? p.getImageUrl() : null;
                if (thumb != null && !thumb.isEmpty()) urls.add(thumb);
            }
            sliderAdapter.updateImages(urls);
            binding.dotsIndicator.setVisibility(urls.size() > 1 ? View.VISIBLE : View.GONE);
        });

        viewModel.getVariants().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null && !response.items.isEmpty()) {
                variantStock.clear();
                for (var variant : response.items) {
                    if (variant.id != null) variantStock.put(variant.id, variant.stockQuantity);
                }
                selectedVariantId = response.items.get(0).id;
                binding.chipGroupVariants.removeAllViews();
                for (var variant : response.items) {
                    com.google.android.material.chip.Chip chip =
                            new com.google.android.material.chip.Chip(requireContext());
                    chip.setText(variant.color != null ? variant.color :
                            (variant.variantName != null ? variant.variantName : variant.name));
                    chip.setCheckable(true);
                    chip.setOnCheckedChangeListener((btn, checked) -> {
                        if (checked) {
                            selectedVariantId = variant.id;
                            updateStockStatus(variant.id);
                        }
                    });
                    binding.chipGroupVariants.addView(chip);
                }
                if (binding.chipGroupVariants.getChildCount() > 0) {
                    ((com.google.android.material.chip.Chip)
                            binding.chipGroupVariants.getChildAt(0)).setChecked(true);
                }
                updateStockStatus(selectedVariantId);
            }
        });

        viewModel.getReviews().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                binding.tvRating.setText(String.format("%.1f", summary.averageRating));
                binding.tvReviewCount.setText(getString(R.string.str_reviews_count_format, summary.totalReviews));
                binding.ratingBar.setRating((float) summary.averageRating);
                binding.tvRating.setText(String.format("%.1f", summary.averageRating));
                binding.tvReviewCount.setText(getString(R.string.review_count, summary.totalReviews));
                binding.layoutRatingRow.setOnClickListener(v ->
                        binding.nestedScrollView.post(() ->
                                binding.nestedScrollView.smoothScrollTo(
                                        0, binding.layoutReviewsSection.getTop())));
                binding.tvNoReviews.setVisibility(View.GONE);
                binding.rvReviews.setVisibility(View.VISIBLE);
                binding.chipGroupReviewSort.setVisibility(View.VISIBLE);
                if (summary.items != null) {
                    allReviews.clear();
                    allReviews.addAll(summary.items);
                    reviewAdapter.submitList(new ArrayList<>(allReviews));
                }
            }
        });

        viewModel.getAddToCartResult().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), R.string.str_added_to_cart_success, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getRecommendations().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                recommendationAdapter.submitList(items);
                binding.layoutRecommendations.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateStockStatus(String variantId) {
        Integer stock = variantStock.get(variantId);
        if (stock == null) {
            binding.tvStockStatus.setVisibility(View.GONE);
            return;
        }
        binding.tvStockStatus.setVisibility(View.VISIBLE);
        if (stock <= 0) {
            binding.tvStockStatus.setText(getString(R.string.out_of_stock));
            binding.tvStockStatus.setTextColor(requireContext().getColor(R.color.discount_red));
            binding.btnAddToCart.setEnabled(false);
        } else {
            binding.tvStockStatus.setText(getString(R.string.in_stock));
            binding.tvStockStatus.setTextColor(requireContext().getColor(R.color.success));
            binding.btnAddToCart.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cartRestoreHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
