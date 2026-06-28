package com.unifurniture.mobile.ui.product;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecentlyViewedManager;
import com.unifurniture.mobile.util.ScrollStateHelper;
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
    private String currentSlug;
    private boolean descExpanded = false;
    private String selectedVariantId = null;
    private int quantity = 1;
    private boolean selectedVariantInStock = true;
    private boolean toolbarVisible = true;
    private final Map<String, Integer> variantStock = new HashMap<>();
    private final List<ReviewDto> allReviews = new ArrayList<>();
    private final Handler cartRestoreHandler = new Handler(Looper.getMainLooper());
    private final ScrollStateHelper scrollState = new ScrollStateHelper("product_detail");

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
        viewModel = NavViewModelProvider.get(this, R.id.productDetailFragment, ProductDetailViewModel.class);
        scrollState.read(savedInstanceState);

        String slug = getArguments() != null ? getArguments().getString("slug") : null;
        if (slug == null) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }
        currentSlug = slug;

        recentlyViewedManager = new RecentlyViewedManager(requireContext());
        setupImageSlider();
        setupReviews();
        setupRecommendations();
        setupReviewSort();
        viewModel.loadProductIfNeeded(slug);
        observeData();
        scrollState.restore(binding.nestedScrollView);
        setupAutoHideToolbar();

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
            com.unifurniture.mobile.util.SoundManager.playSound(requireContext(), com.unifurniture.mobile.util.SoundManager.SOUND_SUCCESS);
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            var response = viewModel.getVariants().getValue();
            if (response != null && response.items != null && !response.items.isEmpty()) {
                if (selectedVariantId == null) {
                    Toast.makeText(requireContext(), R.string.please_select_variant, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            viewModel.addToCart(selectedVariantId, quantity);
            // Navigate to cart after a short delay to allow cart to update
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded()) {
                    Navigation.findNavController(requireView()).navigate(R.id.cartFragment);
                }
            }, 300);
        });
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
        binding.btnFavorite.setOnClickListener(v -> viewModel.toggleWishlist());

        binding.btnToggleDescription.setOnClickListener(v -> {
            androidx.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) binding.getRoot());
            if (descExpanded) {
                // Thu gọn
                binding.tvDescription.setMaxLines(4);
                binding.btnToggleDescription.setText(R.string.read_more);
                binding.btnToggleDescription.setBackgroundResource(R.drawable.bg_btn_toggle_outline);
                binding.btnToggleDescription.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
                binding.btnToggleDescription.setIconResource(R.drawable.ic_arrow_down);
                binding.btnToggleDescription.setIconTintResource(R.color.primary);
                binding.viewDescriptionGradient.setVisibility(View.VISIBLE);
                descExpanded = false;
            } else {
                // Mở rộng
                binding.tvDescription.setMaxLines(Integer.MAX_VALUE);
                binding.btnToggleDescription.setText(R.string.read_less);
                binding.btnToggleDescription.setBackgroundResource(R.drawable.bg_btn_toggle_solid);
                binding.btnToggleDescription.setTextColor(android.graphics.Color.WHITE);
                binding.btnToggleDescription.setIconResource(R.drawable.ic_arrow_up);
                binding.btnToggleDescription.setIconTintResource(R.color.white);
                binding.viewDescriptionGradient.setVisibility(View.GONE);
                descExpanded = true;
            }
        });
    }

    /** Set the description text and re-run the read-more / gradient logic for that content. */
    private void renderDescription(CharSequence content) {
        if (binding == null) return;
        descExpanded = false;
        binding.tvDescription.setText(content);
        binding.tvDescription.setMaxLines(Integer.MAX_VALUE);
        binding.btnToggleDescription.setText(R.string.read_more);
        binding.btnToggleDescription.setBackgroundResource(R.drawable.bg_btn_toggle_outline);
        binding.btnToggleDescription.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        binding.btnToggleDescription.setIconResource(R.drawable.ic_arrow_down);
        binding.btnToggleDescription.setIconTintResource(R.color.primary);
        binding.tvDescription.post(() -> {
            if (binding == null) return;
            int lineCount = binding.tvDescription.getLineCount();
            if (lineCount > 4) {
                binding.tvDescription.setMaxLines(4);
                binding.btnToggleDescription.setVisibility(View.VISIBLE);
                binding.viewDescriptionGradient.setVisibility(View.VISIBLE);
            } else {
                binding.btnToggleDescription.setVisibility(View.GONE);
                binding.viewDescriptionGradient.setVisibility(View.GONE);
            }
        });
    }

    /** Render a description HTML string into a CharSequence (inline images via Glide, cleaned newlines). */
    private CharSequence renderHtml(String html) {
        return cleanHtmlNewlines(HtmlCompat.fromHtml(
                html, HtmlCompat.FROM_HTML_MODE_COMPACT, new GlideImageGetter(binding.tvDescription), null));
    }


    private void setupAutoHideToolbar() {
        binding.toolbarOverlay.post(() -> {
            if (binding == null) return;
            binding.toolbarOverlay.setVisibility(View.VISIBLE);
            binding.toolbarOverlay.setAlpha(1f);
            binding.toolbarOverlay.setTranslationY(0f);
        });

        binding.nestedScrollView.setOnScrollChangeListener((View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            int deltaY = scrollY - oldScrollY;
            int threshold = FormatUtil.dpToPx(requireContext(), 6);

            if (scrollY <= threshold) {
                showToolbarOverlay();
            } else if (deltaY > threshold) {
                hideToolbarOverlay();
            } else if (deltaY < -threshold) {
                showToolbarOverlay();
            }
        });
    }

    private void hideToolbarOverlay() {
        if (!toolbarVisible || binding == null) return;
        toolbarVisible = false;
        binding.toolbarOverlay.animate()
                .translationY(-binding.toolbarOverlay.getHeight())
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    if (binding == null || toolbarVisible) return;
                    binding.toolbarOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    private void showToolbarOverlay() {
        if (toolbarVisible || binding == null) return;
        toolbarVisible = true;
        binding.toolbarOverlay.setVisibility(View.VISIBLE);
        binding.toolbarOverlay.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(180)
                .withEndAction(null)
                .start();
    }

    private void setupImageSlider() {
        sliderAdapter = new ImageSliderAdapter(requireContext(), new ArrayList<>());
        binding.viewPagerImages.setAdapter(sliderAdapter);
        binding.viewPagerImages.setOverScrollMode(View.OVER_SCROLL_NEVER);
        binding.viewPagerImages.setOffscreenPageLimit(3);
        binding.dotsIndicator.attachTo(binding.viewPagerImages);

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
        boolean hasMultiple = total > 1;
        binding.tvImageIndicator.setVisibility(hasMultiple ? View.VISIBLE : View.GONE);
        binding.dotsIndicator.setVisibility(hasMultiple ? View.VISIBLE : View.GONE);
        if (hasMultiple) {
            binding.tvImageIndicator.setText(getString(R.string.image_indicator_format, position + 1, total));
        }
    }

    private void setupReviews() {
        String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
        reviewAdapter = new ReviewAdapter(serverHost, getChildFragmentManager());
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(reviewAdapter);
    }

    private void setupRecommendations() {
        String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
        recommendationAdapter = new ProductCardAdapter(serverHost, product -> {
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
            if (product == null) {
                if (!com.unifurniture.mobile.util.NetworkUtil.isOnline(requireContext())) {
                    com.unifurniture.mobile.util.CustomBlueDialog.showNoInternet(
                            requireContext(), () -> { if (currentSlug != null) viewModel.loadProduct(currentSlug); });
                }
                return;
            }
            binding.tvProductName.setText(product.name);
            
            // Logic hiển thị giá mặc định (khi chưa chọn variant hoặc nếu không có variant)
            updatePricingUI(product.minPrice, product.getEffectiveCompareAtPrice());

            if (product.description != null && !product.description.isEmpty()) {
                // Description comes already translated from the server (product_translations overlay,
                // now Google-translated). Just render it.
                renderDescription(renderHtml(product.description));
            } else {
                binding.tvDescription.setText("");
                binding.btnToggleDescription.setVisibility(View.GONE);
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
            String serverHost = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");
            String thumb = com.unifurniture.mobile.util.CategoryImageHelper.resolveProductUrl(product, serverHost);
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
            String serverHost = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");
            List<String> urls = new ArrayList<>();
            if (response != null && response.items != null) {
                for (var img : response.items) {
                    if (img.imageUrl != null && !img.imageUrl.isEmpty()) {
                        String url = img.imageUrl.replace("http://localhost:3000", serverHost);
                        urls.add(url);
                    }
                }
            }
            if (urls.isEmpty()) {
                ProductDto p = viewModel.getProduct().getValue();
                String thumb = com.unifurniture.mobile.util.CategoryImageHelper.resolveProductUrl(p, serverHost);
                if (thumb != null && !thumb.isEmpty()) urls.add(thumb);
            }
            sliderAdapter.updateImages(urls);
            if (!urls.isEmpty()) {
                binding.viewPagerImages.setCurrentItem(0, false);
            }
            updateImageIndicator(binding.viewPagerImages.getCurrentItem());
        });

        viewModel.getVariants().observe(getViewLifecycleOwner(), response -> {
            variantStock.clear();
            binding.chipGroupVariants.removeAllViews();
            selectedVariantId = null;

            List<com.unifurniture.mobile.data.model.ProductVariantDto> validVariants =
                    getRenderableVariants(response != null ? response.items : null);

            if (!validVariants.isEmpty()) {
                // Find index of variant with the highest discount percentage
                int selectedIndex = 0;
                double maxDiscountPct = 0.0;
                for (int i = 0; i < validVariants.size(); i++) {
                    var variant = validVariants.get(i);
                    if (variant.price != null && variant.compareAtPrice != null && variant.compareAtPrice > variant.price) {
                        double pct = (variant.compareAtPrice - variant.price) / variant.compareAtPrice;
                        if (pct > maxDiscountPct) {
                            maxDiscountPct = pct;
                            selectedIndex = i;
                        }
                    }
                }

                int idx = 0;
                for (var variant : validVariants) {
                    if (variant.id != null) {
                        variantStock.put(variant.id, variant.stockQuantity != null ? variant.stockQuantity : 0);
                    }

                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
                    chip.setId(View.generateViewId());
                    chip.setText(getVariantChipText(variant));
                    chip.setCheckable(true);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
                    chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_300)));
                    chip.setChipStrokeWidth(FormatUtil.dpToPx(requireContext(), 1));
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

                    chip.setOnCheckedChangeListener((btn, checked) -> {
                        if (checked) {
                            selectedVariantId = variant.id;
                            updateStockStatus(variant.id);
                            updatePricingUI(variant.price, variant.compareAtPrice);
                            updateVariantChipStyle((com.google.android.material.chip.Chip) btn, true);
                        } else {
                            updateVariantChipStyle((com.google.android.material.chip.Chip) btn, false);
                        }
                    });

                    binding.chipGroupVariants.addView(chip);
                    if (idx == selectedIndex) {
                        chip.setChecked(true);
                    }
                    idx++;
                }

                binding.layoutVariantsSection.setVisibility(View.VISIBLE);
                binding.chipGroupVariants.setVisibility(View.VISIBLE);
            } else {
                binding.layoutVariantsSection.setVisibility(View.GONE);
                binding.chipGroupVariants.setVisibility(View.GONE);
                selectedVariantInStock = true;
                updateStockStatus(null);
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
                binding.btnAddToCart.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                binding.btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.success)));
                cartRestoreHandler.removeCallbacksAndMessages(null);
                cartRestoreHandler.postDelayed(() -> {
                    if (binding == null) return;
                    binding.btnAddToCart.setEnabled(selectedVariantInStock);
                    binding.btnAddToCart.setText(R.string.add_to_cart);
                    restoreAddToCartButtonStyle();
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
                if (getString(R.string.added_to_favorites).contentEquals(message)) {
                    showWishlistAddedDialog();
                } else {
                    com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
                viewModel.clearSnackbarMessage();
            }
        });
    }

    private List<com.unifurniture.mobile.data.model.ProductVariantDto> getRenderableVariants(
            List<com.unifurniture.mobile.data.model.ProductVariantDto> variants) {
        List<com.unifurniture.mobile.data.model.ProductVariantDto> valid = new ArrayList<>();
        ProductDto product = viewModel.getProduct().getValue();
        String productId = product != null ? product.id : null;
        if (variants == null || productId == null) return valid;

        for (var variant : variants) {
            if (variant == null || variant.id == null || variant.id.trim().isEmpty()) continue;
            String variantProductId = variant.getProductId();
            if (variantProductId == null || !productId.equals(variantProductId)) continue;
            String label = getRawVariantLabel(variant);
            if (label == null || label.trim().isEmpty()) continue;
            valid.add(variant);
        }
        return valid;
    }

    private void showWishlistAddedDialog() {
        if (!isAdded()) return;
        com.unifurniture.mobile.util.CustomBlueDialog.show(
                requireContext(),
                R.drawable.ic_favorite_filled,
                getString(R.string.added_to_favorites),
                "",
                getString(R.string.view_wishlist),
                dialog -> {
                    dialog.dismiss();
                    Navigation.findNavController(requireView()).navigate(R.id.wishlistFragment);
                },
                getString(R.string.close),
                dialog -> dialog.dismiss());
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

    private void updatePricingUI(Double currentPrice, Double originalPrice) {
        if (currentPrice == null) {
            binding.tvPrice.setText(R.string.loading);
            binding.tvPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_800));
            binding.tvOriginalPrice.setVisibility(View.GONE);
            binding.tvDiscount.setVisibility(View.GONE);
            return;
        }

        binding.tvPrice.setText(FormatUtil.formatCurrency(currentPrice));

        if (originalPrice != null && originalPrice > currentPrice) {
            binding.tvPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.discount_red));
            binding.tvOriginalPrice.setText(FormatUtil.formatCurrency(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvOriginalPrice.setVisibility(View.VISIBLE);

            String badge = FormatUtil.discountBadge(currentPrice, originalPrice);
            if (badge != null) {
                binding.tvDiscount.setText(badge);
                binding.tvDiscount.setVisibility(View.VISIBLE);
            } else {
                binding.tvDiscount.setVisibility(View.GONE);
            }
        } else {
            binding.tvPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            binding.tvOriginalPrice.setVisibility(View.GONE);
            binding.tvDiscount.setVisibility(View.GONE);
        }
    }

    private void updateStockStatus(String variantId) {
        Integer stock = variantStock.get(variantId);
        if (stock == null) {
            binding.tvStockStatus.setVisibility(View.GONE);
            return;
        }
        binding.tvStockStatus.setVisibility(View.VISIBLE);
        if (stock <= 0) {
            selectedVariantInStock = false;
            binding.tvStockStatus.setText(getString(R.string.out_of_stock));
            binding.tvStockStatus.setTextColor(requireContext().getColor(R.color.discount_red));
            binding.btnAddToCart.setEnabled(false);
            binding.btnAddToCart.setText(R.string.add_to_cart);
            restoreAddToCartButtonStyle();
        } else {
            selectedVariantInStock = true;
            binding.tvStockStatus.setText(getString(R.string.in_stock));
            binding.tvStockStatus.setTextColor(requireContext().getColor(R.color.success));
            binding.btnAddToCart.setEnabled(true);
            binding.btnAddToCart.setText(R.string.add_to_cart);
            restoreAddToCartButtonStyle();
        }
    }

    private String getVariantLabelText(com.unifurniture.mobile.data.model.ProductVariantDto variant) {
        if (variant == null) {
            return getString(R.string.variant_default_name);
        }
        String label = FormatUtil.getVariantLabel(variant);
        if (label == null || label.trim().isEmpty() || label.equalsIgnoreCase("Mặc định")) {
            return getString(R.string.variant_default_name);
        }
        return label;
    }

    private String getVariantChipText(com.unifurniture.mobile.data.model.ProductVariantDto variant) {
        String label = getRawVariantLabel(variant);
        return label != null ? label : "";
    }

    private String getRawVariantLabel(com.unifurniture.mobile.data.model.ProductVariantDto variant) {
        if (variant == null) return null;
        String color = safeVariantText(variant.color);
        String spec = safeVariantText(variant.variantName);
        if (spec == null) spec = safeVariantText(variant.name);
        if (spec == null) spec = safeVariantText(variant.label);

        if (color != null && spec != null) {
            if (color.equalsIgnoreCase(spec) || spec.toLowerCase().contains(color.toLowerCase())) return spec;
            if (color.toLowerCase().contains(spec.toLowerCase())) return color;
            return color + " - " + spec;
        }
        return color != null ? color : spec;
    }

    private String safeVariantText(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty() || "undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private void restoreAddToCartButtonStyle() {
        if (binding == null) return;
        int gold = ContextCompat.getColor(requireContext(), R.color.accent);
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        binding.btnAddToCart.setTextColor(white);
        binding.btnAddToCart.setIconTint(ColorStateList.valueOf(white));
        binding.btnAddToCart.setStrokeWidth(0);
        binding.btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(gold));
    }

    private void updateVariantChipStyle(com.google.android.material.chip.Chip chip, boolean selected) {
        if (selected) {
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_alpha_10)));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        } else {
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_300)));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null) scrollState.saveScroll(binding.nestedScrollView);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            scrollState.save(outState, binding.nestedScrollView);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cartRestoreHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
