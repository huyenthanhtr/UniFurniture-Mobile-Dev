package com.unifurniture.mobile.ui.cart;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentCartBinding;
import com.unifurniture.mobile.ui.MainActivity;
import com.unifurniture.mobile.ui.adapter.CartItemAdapter;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.VoucherManager;
import com.unifurniture.mobile.data.model.VoucherDto;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartItemAdapter adapter;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("cart");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.cartFragment, CartViewModel.class);
        setupRecyclerView(savedInstanceState);
        observeData();

        binding.cbSelectAll.setOnCheckedChangeListener((btn, checked) -> {
            var cart = viewModel.getCart().getValue();
            if (cart != null && cart.items != null) {
                for (var item : cart.items) {
                    item.isSelected = checked;
                }
                adapter.notifyDataSetChanged();
                refreshPricing();
            }
        });

        binding.btnDeleteAll.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_all)
                    .setMessage("Bạn có chắc chắn muốn xóa tất cả sản phẩm trong giỏ hàng?")
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        var cart = viewModel.getCart().getValue();
                        if (cart != null && cart.items != null) {
                            for (var item : new java.util.ArrayList<>(cart.items)) {
                                viewModel.removeItem(item.id);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        binding.layoutVoucher.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("subtotal", getSelectedTotal());
            bundle.putString("entry_mode", "apply");
            Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment, bundle);
        });

        binding.btnRemoveVoucher.setOnClickListener(v -> {
            VoucherManager.getInstance(requireContext()).clearSelectedVoucherCode();
            refreshPricing();
        });

        binding.btnCheckout.setOnClickListener(v -> {
            var selectedItems = getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle bundle = new Bundle();
            VoucherDto selectedVoucher = VoucherManager.getInstance(requireContext()).getSelectedVoucher();
            if (selectedVoucher != null) {
                bundle.putString("coupon_code", selectedVoucher.code);
            }
            bundle.putString("cart_items_json", new com.google.gson.Gson().toJson(selectedItems));
            Navigation.findNavController(view).navigate(R.id.checkoutFragment, bundle);
        });

        binding.btnLoginPrompt.setOnClickListener(v ->
                ((MainActivity) requireActivity()).launchAuth());
    }

    private java.util.List<com.unifurniture.mobile.data.model.CartItemDto> getSelectedItems() {
        java.util.List<com.unifurniture.mobile.data.model.CartItemDto> selected = new java.util.ArrayList<>();
        var cart = viewModel.getCart().getValue();
        if (cart != null && cart.items != null) {
            for (var item : cart.items) {
                if (item.isSelected) selected.add(item);
            }
        }
        return selected;
    }

    private double getSelectedTotal() {
        double total = 0;
        for (var item : getSelectedItems()) {
            total += item.getTotalPrice();
        }
        return total;
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        String serverHost = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");
        adapter = new CartItemAdapter(
                serverHost,
                (item, quantity) -> viewModel.updateQuantity(item.id, quantity),
                item -> viewModel.removeItem(item.id),
                item -> showVariantSelectionDialog(item),
                item -> {
                    var product = item.getProduct();
                    if (product != null && product.slug != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("slug", product.slug);
                        Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, bundle);
                    }
                },
                (item, isSelected) -> {
                    updateSelectAllState();
                    refreshPricing();
                }
        );
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setAdapter(adapter);
        rvState.bind(binding.rvCartItems, savedInstanceState);
    }

    private void updateSelectAllState() {
        var cart = viewModel.getCart().getValue();
        if (cart == null || cart.items == null || cart.items.isEmpty()) return;
        
        boolean allSelected = true;
        for (var item : cart.items) {
            if (!item.isSelected) {
                allSelected = false;
                break;
            }
        }
        binding.cbSelectAll.setOnCheckedChangeListener(null);
        binding.cbSelectAll.setChecked(allSelected);
        binding.cbSelectAll.setOnCheckedChangeListener((btn, checked) -> {
            for (var item : cart.items) item.isSelected = checked;
            adapter.notifyDataSetChanged();
            refreshPricing();
        });
    }

    private void showVariantSelectionDialog(com.unifurniture.mobile.data.model.CartItemDto item) {
        if (item == null) return;
        var product = item.getProduct();
        if (product == null || product.id == null) {
            Toast.makeText(getContext(), getString(R.string.toast_product_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.getProductVariants(product.id).observe(getViewLifecycleOwner(), variants -> {
            if (variants == null || variants.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_no_other_variants), Toast.LENGTH_SHORT).show();
                return;
            }

            String[] variantNames = new String[variants.size()];
            String currentVariantId = item.getVariantId();
            int selectedIndex = -1;

            for (int i = 0; i < variants.size(); i++) {
                var v = variants.get(i);
                String label = FormatUtil.getVariantLabel(v);
                if (v.price != null) {
                    label += " - " + FormatUtil.formatCurrency(v.price);
                }
                variantNames[i] = label;
                if (currentVariantId != null && currentVariantId.equals(v.id)) {
                    selectedIndex = i;
                }
            }

            final int[] tempSelected = {selectedIndex};

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_variant)
                    .setSingleChoiceItems(variantNames, selectedIndex, (dialog, which) -> {
                        tempSelected[0] = which;
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (tempSelected[0] >= 0 && tempSelected[0] < variants.size()) {
                            var selectedVariant = variants.get(tempSelected[0]);
                            if (currentVariantId == null || !currentVariantId.equals(selectedVariant.id)) {
                                viewModel.updateCartItemVariant(item.id, selectedVariant.id);
                            }
                        }
                    })
                    .setNegativeButton(R.string.close, null)
                    .show();
        });
    }

    private void observeData() {
        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            boolean isEmpty = cart == null || cart.items == null || cart.items.isEmpty();
            binding.layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.layoutCart.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.cardCheckout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.cardHeader.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (isEmpty) {
                Context context = getContext();
                if (context != null) {
                    boolean isLoggedIn = SessionManager.getInstance(context).isLoggedIn();
                    binding.btnLoginPrompt.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
                } else {
                    binding.btnLoginPrompt.setVisibility(View.VISIBLE);
                }
            } else {
                adapter.submitList(cart.items, () -> {
                    rvState.afterSubmitCallback().run();
                    updateSelectAllState();
                    refreshPricing();
                });
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean initialLoad = Boolean.TRUE.equals(loading)
                    && (viewModel.getCart().getValue() == null
                    || viewModel.getCart().getValue().items == null
                    || viewModel.getCart().getValue().items.isEmpty());
            binding.progressBar.setVisibility(initialLoad ? View.VISIBLE : View.GONE);
        });
    }

    private void refreshPricing() {
        if (binding == null || viewModel == null) return;
        Context context = getContext();
        if (context == null) return;

        double subtotal = getSelectedTotal();
        int selectedCount = getSelectedItems().size();
        binding.tvTotalItems.setText(getString(R.string.total_items_format, selectedCount));

        if (subtotal <= 0) {
            VoucherManager.getInstance(context).clearSelectedVoucherCode();
            binding.layoutPricingDetails.setVisibility(View.GONE);
            binding.tvTotal.setText(FormatUtil.formatCurrency(0.0));
            return;
        }

        VoucherManager voucherMgr = VoucherManager.getInstance(context);
        VoucherDto voucher = voucherMgr.getSelectedVoucher();

        if (voucher != null) {
            // Check eligibility in case cart quantity changed
            if (subtotal < voucher.minOrderValue) {
                voucherMgr.clearSelectedVoucherCode();
                voucher = null;
                Toast.makeText(context, getString(R.string.toast_voucher_removed_min_spend), Toast.LENGTH_SHORT).show();
            }
        }

        if (voucher != null) {
            double discount = voucherMgr.calculateDiscount(voucher, subtotal);
            binding.layoutPricingDetails.setVisibility(View.VISIBLE);
            binding.tvSubtotal.setText(FormatUtil.formatCurrency(subtotal));
            binding.tvDiscount.setText("-" + FormatUtil.formatCurrency(discount));
            binding.tvTotal.setText(FormatUtil.formatCurrency(subtotal - discount));

            binding.tvVoucherSelector.setText(getString(R.string.toast_voucher_applied, voucher.code));
            binding.tvVoucherSelector.setTextColor(ContextCompat.getColor(context, R.color.primary));
            binding.btnRemoveVoucher.setVisibility(View.VISIBLE);
            binding.ivVoucherArrow.setVisibility(View.GONE);
        } else {
            binding.layoutPricingDetails.setVisibility(View.GONE);
            binding.tvTotal.setText(FormatUtil.formatCurrency(subtotal));

            binding.tvVoucherSelector.setText(R.string.select_voucher);
            binding.tvVoucherSelector.setTextColor(ContextCompat.getColor(context, R.color.black));
            binding.btnRemoveVoucher.setVisibility(View.GONE);
            binding.ivVoucherArrow.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadCart();
        refreshPricing();
    }

    @Override
    public void onStop() {
        super.onStop();
        rvState.savePending();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        rvState.save(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
