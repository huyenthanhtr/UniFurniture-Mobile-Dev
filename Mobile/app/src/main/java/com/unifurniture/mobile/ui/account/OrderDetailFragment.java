package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.OrderDetailDto;
import com.unifurniture.mobile.data.model.OrderDetailResponse;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.data.model.PaymentDto;
import com.unifurniture.mobile.data.model.PaymentSummaryDto;
import com.unifurniture.mobile.data.model.PricingDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentOrderDetailBinding;
import com.unifurniture.mobile.databinding.ItemOrderDetailBinding;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.OrderStatusUi;
import com.unifurniture.mobile.util.ToastUtil;
import java.text.Normalizer;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private ApiService apiService;
    private String orderId;
    private boolean pricingExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.pricingHeader.setOnClickListener(v -> togglePricingDetails());

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id");
        }

        if (orderId != null) {
            loadOrderDetails();
        } else {
            ToastUtil.error(requireContext(), R.string.error_unknown);
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void loadOrderDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentScrollView.setVisibility(View.GONE);

        apiService.getOrderById(orderId).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(Call<OrderDetailResponse> call, Response<OrderDetailResponse> response) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && response.body().getOrder() != null) {
                        binding.contentScrollView.setVisibility(View.VISIBLE);
                        displayOrderInfo(response.body());
                    } else {
                        ToastUtil.error(requireContext(), R.string.error_unknown);
                    }
                }
            }

            @Override
            public void onFailure(Call<OrderDetailResponse> call, Throwable t) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
                }
            }
        });
    }

    private void displayOrderInfo(OrderDetailResponse detailResponse) {
        OrderDto order = detailResponse.getOrder();
        String displayCode = order.getOrderCode();
        if (displayCode == null || displayCode.trim().isEmpty()) {
            String id = order.getId();
            displayCode = id != null && !id.isEmpty()
                    ? "#" + id.substring(Math.max(0, id.length() - 8))
                    : getString(R.string.order_details);
        }
        binding.tvOrderId.setText(getString(R.string.order_number_format, displayCode));
        OrderStatusUi.applyBadge(binding.tvOrderStatus, order.getStatus());
        binding.tvOrderDate.setText(getString(R.string.order_date_format, formatDate(order.getCreatedAt())));
        
        binding.tvShippingName.setText(safeText(order.getShippingName()));
        binding.tvShippingPhone.setText(safeText(order.getShippingPhone()));
        binding.tvShippingAddress.setText(safeText(order.getShippingAddress()));
        
        PaymentSummaryDto summary = order.getPaymentSummary() != null
                ? order.getPaymentSummary()
                : detailResponse.getPaymentSummary();
        PaymentDto latestPayment = detailResponse.getPayments() != null && !detailResponse.getPayments().isEmpty()
                ? detailResponse.getPayments().get(0)
                : null;

        String method = firstNonBlank(
                order.getPaymentMethod(),
                summary != null ? summary.getMethod() : null,
                latestPayment != null ? latestPayment.getMethod() : null
        );
        binding.tvPaymentMethod.setText(formatPaymentMethod(method));

        String pStatus = firstNonBlank(
                order.getPaymentStatus(),
                summary != null ? summary.getStatus() : null,
                latestPayment != null ? latestPayment.getStatus() : null
        );
        binding.tvPaymentStatus.setText(formatPaymentStatus(pStatus));
        
        binding.llOrderItems.removeAllViews();
        List<OrderDetailDto> details = detailResponse.getItems() != null ? detailResponse.getItems() : order.getDetails();
        if (details != null && !details.isEmpty()) {
            for (OrderDetailDto item : details) {
                ItemOrderDetailBinding itemBinding = ItemOrderDetailBinding.inflate(getLayoutInflater(), binding.llOrderItems, true);
                
                itemBinding.tvProductName.setText(resolveProductTitle(item));
                itemBinding.tvVariantName.setText(resolveVariantTitle(item));
                
                itemBinding.tvPrice.setText(FormatUtil.formatCurrency(item.getUnitPrice()));
                itemBinding.tvQuantity.setText(getString(R.string.quantity_format, item.getQuantity() != null ? item.getQuantity() : 1));

                String imageUrl = item.getImageUrl();
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_product)
                            .error(R.drawable.placeholder_product)
                            .into(itemBinding.ivProductImage);
                } else if (item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                    Glide.with(this)
                            .load(item.getProduct().getImages().get(0).getUrl())
                            .placeholder(R.drawable.placeholder_product)
                            .error(R.drawable.placeholder_product)
                            .into(itemBinding.ivProductImage);
                } else {
                    itemBinding.ivProductImage.setImageResource(R.drawable.placeholder_product);
                }

                itemBinding.layoutProductLink.setOnClickListener(v -> openProductDetail(item));
            }
        }

        pricingExpanded = false;
        bindPricingSummary(order, detailResponse, details);
        updateTimeline(order.getStatus());
    }

    private void bindPricingSummary(OrderDto order, OrderDetailResponse detailResponse, List<OrderDetailDto> details) {
        PricingDto pricing = detailResponse.getPricing();
        double total = positiveAmount(pricing != null ? pricing.getGrandTotal() : null);
        if (total <= 0) total = positiveAmount(order.getTotalAmount());
        if (total <= 0 && order.getPaymentSummary() != null) {
            total = positiveAmount(order.getPaymentSummary().getTotalAmount());
        }
        if (total <= 0 && detailResponse.getPaymentSummary() != null) {
            total = positiveAmount(detailResponse.getPaymentSummary().getTotalAmount());
        }

        double subtotal = positiveAmount(pricing != null ? pricing.getItemsSubtotal() : null);
        if (subtotal <= 0) subtotal = calculateItemsSubtotal(details);

        double discount = nonNegativeAmount(pricing != null ? pricing.getDiscountAmount() : null);
        if (discount <= 0 && subtotal > total && total > 0) {
            discount = subtotal - total;
        }
        if (subtotal <= 0 && total > 0) {
            subtotal = Math.max(0, total + discount);
        }

        double deposit = resolveDepositAmount(order, detailResponse);

        binding.tvTotalAmount.setText(FormatUtil.formatCurrency(total));
        binding.tvItemsSubtotal.setText(FormatUtil.formatCurrency(subtotal));
        binding.rowDeposit.setVisibility(deposit > 0 ? View.VISIBLE : View.GONE);
        binding.tvDepositAmount.setText(FormatUtil.formatCurrency(deposit));

        String couponCode = pricing != null ? pricing.getCouponCode() : null;
        boolean hasDiscount = discount > 0 || (couponCode != null && !couponCode.trim().isEmpty());
        binding.rowDiscount.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
        if (hasDiscount) {
            binding.tvDiscountLabel.setText(couponCode != null && !couponCode.trim().isEmpty()
                    ? getString(R.string.order_promotion_with_code, couponCode.trim())
                    : getString(R.string.order_promotion_discount));
            binding.tvDiscountAmount.setText(discount > 0
                    ? "-" + FormatUtil.formatCurrency(discount)
                    : FormatUtil.formatCurrency(0));
        }

        updatePricingExpandedState(false);
    }

    private double calculateItemsSubtotal(List<OrderDetailDto> details) {
        if (details == null) return 0;
        double subtotal = 0;
        for (OrderDetailDto item : details) {
            if (item == null) continue;
            double lineTotal = positiveAmount(item.getTotal());
            if (lineTotal <= 0) {
                int quantity = item.getQuantity() != null ? Math.max(1, item.getQuantity()) : 1;
                lineTotal = positiveAmount(item.getUnitPrice()) * quantity;
            }
            subtotal += lineTotal;
        }
        return subtotal;
    }

    private double resolveDepositAmount(OrderDto order, OrderDetailResponse detailResponse) {
        double deposit = positiveAmount(order != null ? order.getDepositAmount() : null);
        if (deposit <= 0 && order != null && order.getPaymentSummary() != null) {
            deposit = positiveAmount(order.getPaymentSummary().getDepositAmount());
        }
        if (deposit <= 0 && detailResponse != null && detailResponse.getPaymentSummary() != null) {
            deposit = positiveAmount(detailResponse.getPaymentSummary().getDepositAmount());
        }
        return deposit;
    }

    private double positiveAmount(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return 0;
        return value > 0 ? value : 0;
    }

    private double nonNegativeAmount(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return 0;
        return Math.max(0, value);
    }

    private void togglePricingDetails() {
        pricingExpanded = !pricingExpanded;
        updatePricingExpandedState(true);
    }

    private void updatePricingExpandedState(boolean animate) {
        if (binding == null) return;
        binding.llPricingDetails.setVisibility(pricingExpanded ? View.VISIBLE : View.GONE);
        if (animate) {
            binding.ivPricingChevron.animate().rotation(pricingExpanded ? 180f : 0f).setDuration(160).start();
        } else {
            binding.ivPricingChevron.setRotation(pricingExpanded ? 180f : 0f);
        }
    }

    private void openProductDetail(OrderDetailDto item) {
        String slug = null;
        ProductDto product = item != null ? item.getProduct() : null;
        if (product != null) {
            slug = firstNonBlank(product.slug);
        }
        if (slug == null && item != null) {
            slug = firstNonBlank(item.getProductSlug(), item.getProductId());
        }
        if (slug == null) return;

        Bundle args = new Bundle();
        args.putString("slug", slug);
        Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
    }

    private String resolveProductTitle(OrderDetailDto item) {
        ProductDto product = item != null ? item.getProduct() : null;
        String title = firstNonBlank(
                item != null ? item.getProductName() : null,
                product != null ? product.getTitle() : null
        );
        return title != null ? title : getString(R.string.product_default_name);
    }

    private String resolveVariantTitle(OrderDetailDto item) {
        String variantName = item != null ? item.getVariantName() : null;
        if (isDefaultVariantName(variantName)) {
            return getString(R.string.variant_default_name);
        }
        return variantName.trim();
    }

    private boolean isDefaultVariantName(String value) {
        if (value == null) return true;
        String normalized = value.trim().toLowerCase();
        String folded = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.isEmpty()
                || "-".equals(normalized)
                || "default".equals(normalized)
                || "mac dinh".equals(folded);
    }

    private String safeText(String value) {
        return value != null && !value.trim().isEmpty() ? value : "-";
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !"-".equals(value.trim())) {
                return value;
            }
        }
        return null;
    }

    private String formatPaymentMethod(String method) {
        String normalized = method == null ? "" : method.trim().toLowerCase();
        switch (normalized) {
            case "cod":
                return getString(R.string.cod_short);
            case "bank_transfer":
            case "chuyen_khoan":
            case "transfer":
                return getString(R.string.bank_transfer);
            default:
                return safeText(method);
        }
    }

    private String formatPaymentStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        switch (normalized) {
            case "paid":
            case "completed":
                return getString(R.string.payment_paid);
            case "pending":
            case "unpaid":
                return getString(R.string.payment_pending);
            case "failed":
            case "cancelled":
                return getString(R.string.payment_failed);
            default:
                return safeText(status);
        }
    }

    private String formatDate(String value) {
        return value != null && !value.trim().isEmpty()
                ? value.substring(0, Math.min(10, value.length()))
                : "-";
    }

    private void updateTimeline(String status) {
        OrderStatusUi.applyTimeline(
                status,
                binding.ivStep1, binding.tvStep1, binding.line1,
                binding.ivStep2, binding.tvStep2, binding.line2,
                binding.ivStep3, binding.tvStep3, binding.line3,
                binding.ivStep4, binding.tvStep4
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
