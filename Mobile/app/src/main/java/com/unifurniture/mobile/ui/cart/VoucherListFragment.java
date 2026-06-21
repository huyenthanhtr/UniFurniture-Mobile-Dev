package com.unifurniture.mobile.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.databinding.FragmentVoucherListBinding;
import com.unifurniture.mobile.data.model.CouponDto;
import com.unifurniture.mobile.data.model.VoucherDto;
import com.unifurniture.mobile.data.repository.ProductRepository;
import com.unifurniture.mobile.ui.adapter.VoucherAdapter;
import com.unifurniture.mobile.util.CustomBlueDialog;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;
import com.unifurniture.mobile.util.VoucherManager;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.R;
import java.util.ArrayList;
import java.util.List;

public class VoucherListFragment extends Fragment {

    private FragmentVoucherListBinding binding;
    private VoucherListViewModel viewModel;
    private VoucherAdapter adapter;
    private double cartSubtotal = 0;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("vouchers");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVoucherListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = NavViewModelProvider.get(this, R.id.voucherListFragment, VoucherListViewModel.class);

        if (getArguments() != null) {
            cartSubtotal = getArguments().getDouble("subtotal", 0);
        }

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        setupRecyclerView(savedInstanceState);
        observeCoupons();
        viewModel.loadCouponsIfNeeded();
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        adapter = new VoucherAdapter(cartSubtotal, this::handleVoucherSelection);
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVouchers.setAdapter(adapter);
        rvState.bind(binding.rvVouchers, savedInstanceState);
    }

    private void observeCoupons() {
        viewModel.getCoupons().observe(getViewLifecycleOwner(), coupons -> {
            if (coupons == null) return;
            bindVoucherList(coupons);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean initialLoad = Boolean.TRUE.equals(loading) && viewModel.getCoupons().getValue() == null;
            if (initialLoad) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.rvVouchers.setVisibility(View.GONE);
                binding.layoutOptimalContainer.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void bindVoucherList(List<CouponDto> coupons) {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.GONE);

        List<VoucherDto> activeVouchers = new ArrayList<>();
        if (coupons != null) {
            for (CouponDto c : coupons) {
                VoucherDto v = VoucherManager.convertCouponToVoucher(requireContext(), c);
                if (v != null && !v.isUsed) {
                    activeVouchers.add(v);
                }
            }
        }

        VoucherManager.getInstance(requireContext()).saveVouchers(activeVouchers);

        VoucherDto optimalVoucher = null;
        double maxDiscount = 0;
        if (cartSubtotal > 0 && !activeVouchers.isEmpty()) {
            for (VoucherDto v : activeVouchers) {
                if (cartSubtotal >= v.minOrderValue) {
                    double discount = VoucherManager.getInstance(requireContext()).calculateDiscount(v, cartSubtotal);
                    if (discount > maxDiscount) {
                        maxDiscount = discount;
                        optimalVoucher = v;
                    }
                }
            }
        }

        if (optimalVoucher != null && maxDiscount > 0) {
            bindOptimalVoucher(optimalVoucher, maxDiscount);
        } else {
            binding.layoutOptimalContainer.setVisibility(View.GONE);
        }

        List<VoucherDto> displayVouchers = new ArrayList<>();
        for (VoucherDto v : activeVouchers) {
            if (optimalVoucher == null || maxDiscount <= 0 || !v.code.equals(optimalVoucher.code)) {
                displayVouchers.add(v);
            }
        }

        adapter.submitList(displayVouchers, rvState.afterSubmitCallback());

        if (displayVouchers.isEmpty() && (optimalVoucher == null || maxDiscount <= 0)) {
            binding.rvVouchers.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvVouchers.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void bindOptimalVoucher(VoucherDto optimal, double savings) {
        binding.tvOptimalCode.setText(optimal.code);
        binding.tvOptimalName.setText(optimal.name);
        binding.tvOptimalDescription.setText(optimal.description);
        binding.tvOptimalExpiry.setText(optimal.expirationDate);
        binding.tvOptimalSavings.setText(getString(R.string.optimal_savings, FormatUtil.formatCurrency(savings)));

        binding.btnOptimalUse.setOnClickListener(v -> handleVoucherSelection(optimal));

        binding.layoutOptimalContainer.setVisibility(View.VISIBLE);
    }

    private void handleVoucherSelection(VoucherDto voucher) {
        if (cartSubtotal >= voucher.minOrderValue) {
            CustomBlueDialog.show(
                    requireContext(),
                    R.drawable.ic_check,
                    getString(R.string.voucher_applied_title),
                    getString(R.string.voucher_applied_msg, voucher.code),
                    getString(R.string.btn_use_now),
                    dialog -> {
                        VoucherManager.getInstance(requireContext()).setSelectedVoucherCode(voucher.code);
                        dialog.dismiss();
                        if (cartSubtotal > 0) {
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    },
                    getString(R.string.cancel),
                    dialog -> dialog.dismiss()
            );
        } else {
            CustomBlueDialog.show(
                    requireContext(),
                    R.drawable.ic_check,
                    getString(R.string.voucher_title),
                    getString(R.string.toast_voucher_saved_upsell),
                    getString(R.string.confirm),
                    dialog -> {
                        VoucherManager.getInstance(requireContext()).setSelectedVoucherCode(voucher.code);
                        dialog.dismiss();
                        if (cartSubtotal > 0) {
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    },
                    null,
                    null
            );
        }
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
