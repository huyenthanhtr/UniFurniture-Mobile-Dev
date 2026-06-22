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
        
        String method = order.getPaymentMethod();
        if ((method == null || method.trim().isEmpty()) && order.getPaymentSummary() != null) {
            method = order.getPaymentSummary().getMethod();
        }
        binding.tvPaymentMethod.setText(method != null ? method.toUpperCase() : "");
        
        String pStatus = order.getPaymentStatus();
        if ((pStatus == null || pStatus.trim().isEmpty()) && order.getPaymentSummary() != null) {
            pStatus = order.getPaymentSummary().getStatus();
        }
        binding.tvPaymentStatus.setText(safeText(pStatus));
        
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
