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
                        displayOrderInfo(response.body().getOrder(), response.body().getItems());
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

    private void displayOrderInfo(OrderDto order, List<OrderDetailDto> responseItems) {
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
        
        binding.tvPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod().toUpperCase() : "");
        binding.tvPaymentStatus.setText(safeText(order.getPaymentStatus()));
        
        binding.tvTotalAmount.setText(FormatUtil.formatCurrency(order.getTotalAmount()));

        binding.llOrderItems.removeAllViews();
        List<OrderDetailDto> details = responseItems != null ? responseItems : order.getDetails();
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

    private String formatDate(String value) {
        return value != null && !value.trim().isEmpty()
                ? value.substring(0, Math.min(10, value.length()))
                : "-";
    }

    private void updateTimeline(String status) {
        int primaryColor = getResources().getColor(R.color.primary, null);
        int grayColor = getResources().getColor(R.color.gray_300, null);
        int cancelColor = getResources().getColor(R.color.status_cancelled_text, null);
        String normalizedStatus = OrderStatusUi.normalize(status);

        // Reset all to gray first
        binding.ivStep1.setImageResource(android.R.drawable.ic_menu_edit);
        binding.ivStep1.setColorFilter(grayColor);
        binding.tvStep1.setTextColor(grayColor);
        binding.tvStep1.setText(R.string.status_pending);
        binding.line1.setBackgroundColor(grayColor);

        binding.ivStep2.setImageResource(android.R.drawable.ic_menu_manage);
        binding.ivStep2.setColorFilter(grayColor);
        binding.tvStep2.setTextColor(grayColor);
        binding.tvStep2.setText(R.string.status_confirmed);
        binding.line2.setBackgroundColor(grayColor);

        binding.ivStep3.setImageResource(android.R.drawable.ic_menu_send);
        binding.ivStep3.setColorFilter(grayColor);
        binding.tvStep3.setTextColor(grayColor);
        binding.tvStep3.setText(R.string.status_shipping);
        binding.line3.setBackgroundColor(grayColor);

        binding.ivStep4.setImageResource(android.R.drawable.checkbox_on_background);
        binding.ivStep4.setColorFilter(grayColor);
        binding.tvStep4.setTextColor(grayColor);
        binding.tvStep4.setText(R.string.status_completed);

        if (normalizedStatus == null || normalizedStatus.isEmpty()) return;

        // Apply colors based on status progression
        switch (normalizedStatus) {
            case "pending":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                break;
            case "cancel_pending":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                binding.line1.setBackgroundColor(primaryColor);
                binding.ivStep2.setColorFilter(cancelColor);
                binding.tvStep2.setTextColor(cancelColor);
                binding.tvStep2.setText(R.string.status_cancel_pending);
                break;
            case "confirmed":
            case "processing":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                binding.line1.setBackgroundColor(primaryColor);
                
                binding.ivStep2.setColorFilter(primaryColor);
                binding.tvStep2.setTextColor(primaryColor);
                if ("processing".equals(normalizedStatus)) {
                    binding.tvStep2.setText(R.string.status_processing);
                }
                break;
            case "shipping":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                binding.line1.setBackgroundColor(primaryColor);
                
                binding.ivStep2.setColorFilter(primaryColor);
                binding.tvStep2.setTextColor(primaryColor);
                binding.line2.setBackgroundColor(primaryColor);
                
                binding.ivStep3.setColorFilter(primaryColor);
                binding.tvStep3.setTextColor(primaryColor);
                break;
            case "delivered":
            case "completed":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                binding.line1.setBackgroundColor(primaryColor);
                
                binding.ivStep2.setColorFilter(primaryColor);
                binding.tvStep2.setTextColor(primaryColor);
                binding.line2.setBackgroundColor(primaryColor);
                
                binding.ivStep3.setColorFilter(primaryColor);
                binding.tvStep3.setTextColor(primaryColor);
                binding.line3.setBackgroundColor(primaryColor);
                
                binding.ivStep4.setColorFilter(primaryColor);
                binding.tvStep4.setTextColor(primaryColor);
                binding.tvStep4.setText("delivered".equals(normalizedStatus)
                        ? R.string.status_delivered
                        : R.string.status_completed);
                break;
            case "cancelled":
                binding.ivStep1.setImageResource(android.R.drawable.ic_delete);
                binding.ivStep1.setColorFilter(cancelColor);
                binding.tvStep1.setTextColor(cancelColor);
                binding.tvStep1.setText(R.string.status_cancelled);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
