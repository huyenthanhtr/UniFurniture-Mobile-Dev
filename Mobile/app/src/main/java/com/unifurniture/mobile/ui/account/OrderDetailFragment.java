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
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.databinding.FragmentOrderDetailBinding;
import com.unifurniture.mobile.databinding.ItemOrderDetailBinding;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.ScrollStateHelper;
import java.util.List;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private OrderDetailViewModel viewModel;
    private String orderId;
    private final ScrollStateHelper scrollState = new ScrollStateHelper("order_detail");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.orderDetailFragment, OrderDetailViewModel.class);
        scrollState.read(savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id");
        }

        if (orderId == null) {
            Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        viewModel.getOrderDetail().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            binding.contentScrollView.setVisibility(View.VISIBLE);
            displayOrderInfo(state.order, state.items);
            scrollState.restore(binding.contentScrollView);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean initialLoad = Boolean.TRUE.equals(loading) && viewModel.getOrderDetail().getValue() == null;
            binding.progressBar.setVisibility(initialLoad ? View.VISIBLE : View.GONE);
            if (initialLoad) {
                binding.contentScrollView.setVisibility(View.GONE);
            }
        });

        viewModel.loadOrderIfNeeded(orderId);
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
        binding.tvOrderStatus.setText(order.getStatusLabel());
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

        // Reset all to gray first
        binding.ivStep1.setColorFilter(grayColor);
        binding.tvStep1.setTextColor(grayColor);
        binding.line1.setBackgroundColor(grayColor);

        binding.ivStep2.setColorFilter(grayColor);
        binding.tvStep2.setTextColor(grayColor);
        binding.line2.setBackgroundColor(grayColor);

        binding.ivStep3.setColorFilter(grayColor);
        binding.tvStep3.setTextColor(grayColor);
        binding.line3.setBackgroundColor(grayColor);

        binding.ivStep4.setColorFilter(grayColor);
        binding.tvStep4.setTextColor(grayColor);

        if (status == null) return;

        // Apply colors based on status progression
        switch (status) {
            case "pending":
            case "cancel_requested":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                break;
            case "confirmed":
                binding.ivStep1.setColorFilter(primaryColor);
                binding.tvStep1.setTextColor(primaryColor);
                binding.line1.setBackgroundColor(primaryColor);
                
                binding.ivStep2.setColorFilter(primaryColor);
                binding.tvStep2.setTextColor(primaryColor);
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
                break;
            case "cancelled":
                // If cancelled, maybe make step 1 red and stop there?
                int redColor = getResources().getColor(android.R.color.holo_red_dark, null);
                binding.ivStep1.setColorFilter(redColor);
                binding.tvStep1.setTextColor(redColor);
                binding.tvStep1.setText(R.string.status_cancelled);
                binding.ivStep1.setImageResource(android.R.drawable.ic_delete);
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null) scrollState.saveScroll(binding.contentScrollView);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            scrollState.save(outState, binding.contentScrollView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
