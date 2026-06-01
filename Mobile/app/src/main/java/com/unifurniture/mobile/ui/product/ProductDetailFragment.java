package com.unifurniture.mobile.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentProductDetailBinding;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.ui.adapter.ImageSliderAdapter;
import com.unifurniture.mobile.ui.adapter.ProductCardAdapter;
import com.unifurniture.mobile.ui.adapter.ReviewAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.util.FormatUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailFragment extends Fragment {

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    private ImageSliderAdapter sliderAdapter;
    private ReviewAdapter reviewAdapter;
    private ProductCardAdapter recommendationAdapter;
    private String selectedVariantId = null;
    private int quantity = 1;
    private final Map<String, Integer> variantStock = new HashMap<>();

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

        setupImageSlider();
        setupReviews();
        setupRecommendations();
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
        reviewAdapter = new ReviewAdapter();
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
                binding.tvWarranty.setText("Bảo hành " + product.warrantyMonths + " tháng");
                binding.tvWarranty.setVisibility(View.VISIBLE);
            }
            if (product.sold != null && product.sold > 0) {
                binding.tvSoldCount.setText("Đã bán: " + FormatUtil.formatSold(product.sold));
                binding.tvSoldCount.setVisibility(View.VISIBLE);
            }
            // Fallback: show thumbnail immediately if slider still empty
            String thumb = product.getImageUrl();
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
            if (summary == null) return;
            if (summary.totalReviews == 0) {
                binding.ratingBar.setVisibility(View.GONE);
                binding.tvRating.setVisibility(View.GONE);
                binding.tvReviewCount.setVisibility(View.GONE);
            } else {
                binding.tvRating.setText(String.format("%.1f", summary.averageRating));
                binding.tvReviewCount.setText("(" + summary.totalReviews + " đánh giá)");
                binding.tvReviewCount.setVisibility(View.VISIBLE);
                binding.ratingBar.setVisibility(View.VISIBLE);
                binding.ratingBar.setRating((float) summary.averageRating);
            }
            if (summary.items != null) reviewAdapter.submitList(summary.items);
        });

        viewModel.getAddToCartResult().observe(getViewLifecycleOwner(), cart -> {
            if (cart != null) {
                Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
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
            binding.tvStockStatus.setText("Hết hàng");
            binding.tvStockStatus.setTextColor(requireContext().getColor(R.color.discount_red));
            binding.btnAddToCart.setEnabled(false);
        } else {
            binding.tvStockStatus.setText("Còn hàng");
            binding.tvStockStatus.setTextColor(requireContext().getColor(R.color.success));
            binding.btnAddToCart.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
