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
import com.unifurniture.mobile.ui.adapter.ImageSliderAdapter;
import com.unifurniture.mobile.ui.adapter.ReviewAdapter;
import com.unifurniture.mobile.util.FormatUtil;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailFragment extends Fragment {

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    private ImageSliderAdapter sliderAdapter;
    private ReviewAdapter reviewAdapter;
    private String selectedVariantId = null;
    private int quantity = 1;

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
        });

        viewModel.getImages().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null) {
                List<String> urls = new ArrayList<>();
                for (var img : response.items) {
                    if (img.imageUrl != null && !img.imageUrl.isEmpty()) urls.add(img.imageUrl);
                }
                sliderAdapter.updateImages(urls);
                binding.dotsIndicator.setVisibility(urls.size() > 1 ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getVariants().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null && !response.items.isEmpty()) {
                // Auto-select first variant
                selectedVariantId = response.items.get(0).id;
                // Show variant chips
                binding.chipGroupVariants.removeAllViews();
                for (var variant : response.items) {
                    com.google.android.material.chip.Chip chip =
                            new com.google.android.material.chip.Chip(requireContext());
                    chip.setText(variant.color != null ? variant.color :
                            (variant.variantName != null ? variant.variantName : variant.name));
                    chip.setCheckable(true);
                    chip.setOnCheckedChangeListener((btn, checked) -> {
                        if (checked) selectedVariantId = variant.id;
                    });
                    binding.chipGroupVariants.addView(chip);
                }
                if (binding.chipGroupVariants.getChildCount() > 0) {
                    ((com.google.android.material.chip.Chip)
                            binding.chipGroupVariants.getChildAt(0)).setChecked(true);
                }
            }
        });

        viewModel.getReviews().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                binding.tvRating.setText(String.format("%.1f", summary.averageRating));
                binding.tvReviewCount.setText("(" + summary.totalReviews + " đánh giá)");
                binding.ratingBar.setRating((float) summary.averageRating);
                if (summary.items != null) reviewAdapter.submitList(summary.items);
            }
        });

        viewModel.getAddToCartResult().observe(getViewLifecycleOwner(), cart -> {
            if (cart != null) {
                Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
