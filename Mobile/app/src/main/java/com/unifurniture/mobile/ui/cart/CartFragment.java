package com.unifurniture.mobile.ui.cart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentCartBinding;
import com.unifurniture.mobile.ui.adapter.CartItemAdapter;
import com.unifurniture.mobile.ui.auth.AuthActivity;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.VoucherManager;
import com.unifurniture.mobile.data.model.VoucherDto;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartItemAdapter adapter;

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
        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        setupRecyclerView();
        observeData();

        binding.layoutVoucher.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("subtotal", viewModel.getTotal());
            Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment, bundle);
        });

        binding.btnRemoveVoucher.setOnClickListener(v -> {
            VoucherManager.getInstance(requireContext()).clearSelectedVoucherCode();
            refreshPricing();
        });

        binding.btnCheckout.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            String couponCode = VoucherManager.getInstance(requireContext()).getSelectedVoucherCode();
            if (couponCode != null) {
                bundle.putString("coupon_code", couponCode);
            }
            if (viewModel.getCart().getValue() != null && viewModel.getCart().getValue().items != null) {
                bundle.putString("cart_items_json", new com.google.gson.Gson().toJson(viewModel.getCart().getValue().items));
            }
            Navigation.findNavController(view).navigate(R.id.checkoutFragment, bundle);
        });

        binding.btnLoginPrompt.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AuthActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new CartItemAdapter(
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
                }
        );
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setAdapter(adapter);
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

            if (isEmpty) {
                Context context = getContext();
                if (context != null) {
                    boolean isLoggedIn = SessionManager.getInstance(context).isLoggedIn();
                    binding.btnLoginPrompt.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
                } else {
                    binding.btnLoginPrompt.setVisibility(View.VISIBLE);
                }
            } else {
                adapter.submitList(cart.items);
                refreshPricing();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private void refreshPricing() {
        if (binding == null || viewModel == null) return;
        Context context = getContext();
        if (context == null) return;

        double subtotal = viewModel.getTotal();
        if (subtotal <= 0) {
            VoucherManager.getInstance(context).clearSelectedVoucherCode();
            binding.layoutPricingDetails.setVisibility(View.GONE);
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
        if (viewModel != null) {
            viewModel.loadCart();
        }
        refreshPricing();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
