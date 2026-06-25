package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.OrderDetailDto;
import com.unifurniture.mobile.data.model.OrderDetailResponse;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.data.model.PaymentDto;
import com.unifurniture.mobile.data.model.PaymentSummaryDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentOrderDetailBinding;
import com.unifurniture.mobile.databinding.ItemOrderDetailBinding;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.OrderStatusUi;
import com.unifurniture.mobile.util.ToastUtil;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private ApiService apiService;
    private String orderId;

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
        
        binding.tvTotalAmount.setText(FormatUtil.formatCurrency(order.getTotalAmount()));

        binding.llOrderItems.removeAllViews();
        List<OrderDetailDto> details = detailResponse.getItems() != null ? detailResponse.getItems() : order.getDetails();
        if (details != null && !details.isEmpty()) {
            for (OrderDetailDto item : details) {
                ItemOrderDetailBinding itemBinding = ItemOrderDetailBinding.inflate(getLayoutInflater(), binding.llOrderItems, true);
                
                String title = item.getProductName();
                if ((title == null || title.isEmpty()) && item.getProduct() != null) {
                    title = item.getProduct().getTitle();
                }
                if (title == null || title.isEmpty()) {
                    title = getString(R.string.product_default_name);
                }
                itemBinding.tvProductName.setText(title);
                
                String variantName = item.getVariantName();
                itemBinding.tvVariantName.setText(variantName != null && !variantName.trim().isEmpty()
                        ? variantName
                        : getString(R.string.variant_default_name));
                
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
            }
        }

        updateTimeline(order.getStatus());
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
