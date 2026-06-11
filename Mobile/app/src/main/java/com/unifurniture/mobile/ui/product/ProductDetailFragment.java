package com.unifurniture.mobile.ui.product;

import android.content.Intent;
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
import androidx.core.text.HtmlCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.unifurniture.mobile.util.GlideImageGetter;
import com.unifurniture.mobile.ui.auth.AuthActivity;
import com.unifurniture.mobile.util.SessionManager;
import com.google.android.material.snackbar.Snackbar;
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

        binding.tvQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (!val.isEmpty()) {
                    try {
                        int parsed = Integer.parseInt(val);
                        if (parsed >= 1 && parsed <= 99) {
                            quantity = parsed;
                        } else if (parsed > 99) {
                            quantity = 99;
                            binding.tvQuantity.setText(getString(R.string.max_quantity_value));
                            binding.tvQuantity.setSelection(2);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        });

        binding.tvQuantity.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = binding.tvQuantity.getText().toString().trim();
                if (val.isEmpty() || val.equals("0")) {
                    quantity = 1;
                    binding.tvQuantity.setText(getString(R.string.min_quantity_value));
                }
            }
        });

        binding.btnAddToCart.setOnClickListener(v -> {
            var response = viewModel.getVariants().getValue();
            if (response != null && response.items != null && !response.items.isEmpty()) {
                if (selectedVariantId == null) {
                    Toast.makeText(requireContext(), R.string.please_select_variant, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            viewModel.addToCart(selectedVariantId, quantity);
        });
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
        binding.btnFavorite.setOnClickListener(v -> viewModel.toggleWishlist());
    }

    private void setupImageSlider() {
        sliderAdapter = new ImageSliderAdapter(requireContext(), new ArrayList<>());
        binding.viewPagerImages.setAdapter(sliderAdapter);
        
        // Premium zoom-out and fade page transformer
        binding.viewPagerImages.setPageTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 1) {
                float scaleFactor = Math.max(0.85f, 1 - absPosition * 0.15f);
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
                page.setAlpha(Math.max(0.5f, 1 - absPosition * 0.5f));
            } else {
                page.setAlpha(0f);
            }
        });

        binding.viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageIndicator(position);
            }
        });
    }

    private void updateImageIndicator(int position) {
        if (binding == null) return;
        int total = sliderAdapter.getItemCount();
        if (total <= 1) {
            binding.tvImageIndicator.setVisibility(View.GONE);
        } else {
            binding.tvImageIndicator.setVisibility(View.VISIBLE);
            binding.tvImageIndicator.setText(getString(R.string.image_indicator_format, position + 1, total));
        }
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
            if (product.description != null && !product.description.isEmpty()) {
                binding.tvDescription.setText(cleanHtmlNewlines(HtmlCompat.fromHtml(product.description, HtmlCompat.FROM_HTML_MODE_COMPACT, new GlideImageGetter(binding.tvDescription), null)));
            } else {
                binding.tvDescription.setText("");
            }
            if (product.shortDescription != null && !product.shortDescription.isEmpty()) {
                binding.tvShortDesc.setText(cleanHtmlNewlines(HtmlCompat.fromHtml(product.shortDescription, HtmlCompat.FROM_HTML_MODE_COMPACT, new GlideImageGetter(binding.tvShortDesc), null)));
                binding.tvShortDesc.setVisibility(View.VISIBLE);
            } else {
                binding.tvShortDesc.setVisibility(View.GONE);
            }
            if (product.warrantyMonths != null && product.warrantyMonths > 0) {
                binding.tvWarranty.setText(getString(R.string.warranty, product.warrantyMonths));
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
                updateImageIndicator(0);
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
            updateImageIndicator(binding.viewPagerImages.getCurrentItem());
        });

        viewModel.getVariants().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.items != null && !response.items.isEmpty()) {
                variantStock.clear();
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                for (var variant : response.items) {
                    if (variant.id != null) variantStock.put(variant.id, variant.stockQuantity);
                    if (variant.price != null) {
                        if (variant.price < min) min = variant.price;
                        if (variant.price > max) max = variant.price;
                    }
                }

                // Display min-max price range initially
                if (min != Double.MAX_VALUE && max != Double.MIN_VALUE) {
                    if (min < max) {
                        binding.tvPrice.setText(getString(R.string.price_range_format, FormatUtil.formatCurrency(min), FormatUtil.formatCurrency(max)));
                        binding.tvOriginalPrice.setVisibility(View.GONE);
                        binding.tvDiscount.setVisibility(View.GONE);
                    } else {
                        binding.tvPrice.setText(FormatUtil.formatCurrency(min));
                        ProductDto product = viewModel.getProduct().getValue();
                        if (product != null && product.compareAtPrice != null && product.compareAtPrice > min) {
                            binding.tvOriginalPrice.setText(FormatUtil.formatCurrency(product.compareAtPrice));
                            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                            binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                            String badge = FormatUtil.discountBadge(min, product.compareAtPrice);
                            if (badge != null) {
                                binding.tvDiscount.setText(badge);
                                binding.tvDiscount.setVisibility(View.VISIBLE);
                            }
                        } else {
                            binding.tvOriginalPrice.setVisibility(View.GONE);
                            binding.tvDiscount.setVisibility(View.GONE);
                        }
                    }
                }

                selectedVariantId = null;
                binding.chipGroupVariants.removeAllViews();
                for (var variant : response.items) {
                    com.google.android.material.chip.Chip chip =
                            new com.google.android.material.chip.Chip(requireContext());
                    
                    String chipText = FormatUtil.getVariantLabel(variant);
                    chip.setText(chipText);
                    chip.setCheckable(true);
                    chip.setOnCheckedChangeListener((btn, checked) -> {
                        if (checked) {
                            selectedVariantId = variant.id;
                            updateStockStatus(variant.id);
                            
                            // Dynamic price update upon variant selection
                            if (variant.price != null) {
                                binding.tvPrice.setText(FormatUtil.formatCurrency(variant.price));
                                if (variant.compareAtPrice != null && variant.compareAtPrice > variant.price) {
                                    binding.tvOriginalPrice.setText(FormatUtil.formatCurrency(variant.compareAtPrice));
                                    binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                                    binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                                    String badge = FormatUtil.discountBadge(variant.price, variant.compareAtPrice);
                                    if (badge != null) {
                                        binding.tvDiscount.setText(badge);
                                        binding.tvDiscount.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    binding.tvOriginalPrice.setVisibility(View.GONE);
                                    binding.tvDiscount.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
                    binding.chipGroupVariants.addView(chip);
                }
                
                // If there's only 1 variant, select it by default.
                if (response.items.size() == 1 && binding.chipGroupVariants.getChildCount() > 0) {
                    ((com.google.android.material.chip.Chip)
                            binding.chipGroupVariants.getChildAt(0)).setChecked(true);
                } else {
                    updateStockStatus(null);
                }
            }
        });

        viewModel.getReviews().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null || summary.totalReviews == 0) {
                binding.layoutRatingRow.setVisibility(View.GONE);
                binding.tvNoRating.setVisibility(View.VISIBLE);
                binding.tvNoReviews.setVisibility(View.VISIBLE);
                binding.rvReviews.setVisibility(View.GONE);
            } else {
                binding.layoutRatingRow.setVisibility(View.VISIBLE);
                binding.tvNoRating.setVisibility(View.GONE);
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

        viewModel.getAddToCartResult().observe(getViewLifecycleOwner(), cart -> {
            if (cart != null) {
                // Show premium success dialog popup
                android.app.Dialog dialog = new android.app.Dialog(requireContext());
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_add_to_cart_success);
                
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }
                
                dialog.findViewById(R.id.btnViewCart).setOnClickListener(v -> {
                    dialog.dismiss();
                    Navigation.findNavController(requireView()).navigate(R.id.cartFragment);
                });
                
                dialog.findViewById(R.id.btnDismiss).setOnClickListener(v -> {
                    dialog.dismiss();
                });
                
                dialog.show();

                // Button feedback tạm thời
                binding.btnAddToCart.setEnabled(false);
                binding.btnAddToCart.setText(R.string.btn_added);
                binding.btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.success)));
                cartRestoreHandler.removeCallbacksAndMessages(null);
                cartRestoreHandler.postDelayed(() -> {
                    if (binding == null) return;
                    binding.btnAddToCart.setEnabled(true);
                    binding.btnAddToCart.setText(R.string.add_to_cart);
                    binding.btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.primary)));
                }, 1500);
            } else {
                // Nếu result là null nhưng không có lỗi set trong LiveData error
                if (viewModel.getError().getValue() == null) {
                    Toast.makeText(requireContext(), R.string.add_to_cart_error, Toast.LENGTH_SHORT).show();
                }
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

        viewModel.getIsFavorite().observe(getViewLifecycleOwner(), isFavorite -> {
            binding.btnFavorite.setImageResource(isFavorite ?
                    R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
            if (isFavorite) {
                binding.btnFavorite.setColorFilter(null); // Keep red from drawable
            } else {
                binding.btnFavorite.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black));
            }
        });

        viewModel.getSnackbarMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                viewModel.clearSnackbarMessage();
            }
        });
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    private CharSequence trimSpanned(CharSequence s) {
        if (s == null) return "";
        int start = 0;
        int end = s.length();
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        if (start == 0 && end == s.length()) {
            return s;
        }
        return s.subSequence(start, end);
    }

    private CharSequence cleanHtmlNewlines(CharSequence s) {
        if (s == null) return "";
        CharSequence trimmed = trimSpanned(s);
        if (trimmed.length() == 0) return trimmed;
        android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(trimmed);
        
        // Compress consecutive newlines before any ImageSpan to exactly 1 newline
        android.text.style.ImageSpan[] spans = builder.getSpans(0, builder.length(), android.text.style.ImageSpan.class);
        if (spans != null) {
            for (android.text.style.ImageSpan span : spans) {
                int start = builder.getSpanStart(span);
                int newlineCount = 0;
                int i = start - 1;
                while (i >= 0 && builder.charAt(i) == '\n') {
                    newlineCount++;
                    i--;
                }
                if (newlineCount > 1) {
                    builder.delete(i + 2, start);
                }
            }
        }
        
        // Also compress any general 3+ consecutive newlines in the text to 2 newlines (1 empty line)
        int len = builder.length();
        for (int i = 0; i < len - 2; i++) {
            if (builder.charAt(i) == '\n' && builder.charAt(i + 1) == '\n' && builder.charAt(i + 2) == '\n') {
                builder.delete(i + 2, i + 3);
                len--;
                i--;
            }
        }
        return builder;
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
