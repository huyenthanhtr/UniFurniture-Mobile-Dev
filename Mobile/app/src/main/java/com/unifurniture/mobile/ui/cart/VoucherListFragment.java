package com.unifurniture.mobile.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.databinding.FragmentVoucherListBinding;
import com.unifurniture.mobile.data.model.CouponDto;
import com.unifurniture.mobile.data.model.VoucherDto;
import com.unifurniture.mobile.ui.adapter.VoucherAdapter;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.VoucherManager;
import com.unifurniture.mobile.util.ToastUtil;
import com.unifurniture.mobile.R;
import java.util.ArrayList;
import java.util.List;

public class VoucherListFragment extends Fragment {

    private FragmentVoucherListBinding binding;
    private VoucherAdapter adapter;
    private VoucherAdapter usedAdapter;
    private VoucherListViewModel viewModel;
    private double cartSubtotal = 0;
    private boolean isApplyFlow = false;
    private String entryMode = "browse";

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

        if (getArguments() != null) {
            cartSubtotal = getArguments().getDouble("subtotal", 0);
            entryMode = getArguments().getString("entry_mode", "browse");
        }
        isApplyFlow = "apply".equalsIgnoreCase(entryMode);

        viewModel = new ViewModelProvider(this).get(VoucherListViewModel.class);
        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        setupRecyclerView();
        observeCoupons();
        viewModel.loadCouponsIfNeeded();
        loadVouchers();
    }

    private void setupRecyclerView() {
        adapter = new VoucherAdapter(cartSubtotal, voucher -> {
            if (voucher.isUsed) {
                ToastUtil.show(requireContext(), R.string.voucher_already_used);
                return;
            }
            VoucherManager.getInstance(requireContext()).setSelectedVoucherCode(voucher.code);
            if (cartSubtotal >= voucher.minOrderValue) {
                ToastUtil.show(requireContext(), getString(R.string.toast_voucher_applied, voucher.code));
                if (isApplyFlow) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            } else {
                ToastUtil.show(requireContext(), R.string.toast_voucher_saved_upsell);
            }
        });
        usedAdapter = new VoucherAdapter(cartSubtotal, voucher ->
                ToastUtil.show(requireContext(), R.string.voucher_already_used));
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVouchers.setAdapter(adapter);
        binding.rvUsedVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUsedVouchers.setAdapter(usedAdapter);
    }

    private void loadVouchers() {
        if (binding == null) return;

        VoucherManager voucherManager = VoucherManager.getInstance(requireContext());
        List<VoucherDto> allVouchers = voucherManager.getVouchers();
        List<VoucherDto> activeVouchers = new ArrayList<>();
        List<VoucherDto> usedVouchers = new ArrayList<>();

        for (VoucherDto v : allVouchers) {
            if (v == null) continue;
            if (v.isUsed) {
                usedVouchers.add(v);
            } else {
                activeVouchers.add(v);
            }
        }

        VoucherDto optimalVoucher = findOptimalVoucher(activeVouchers, voucherManager);
        bindOptimalVoucher(optimalVoucher, voucherManager);

        List<VoucherDto> regularVouchers = new ArrayList<>();
        for (VoucherDto voucher : activeVouchers) {
            if (optimalVoucher == null || voucher == null || !voucher.code.equalsIgnoreCase(optimalVoucher.code)) {
                regularVouchers.add(voucher);
            }
        }

        adapter.submitList(regularVouchers);
        usedAdapter.submitList(usedVouchers);
        binding.tvUsedVouchersTitle.setVisibility(usedVouchers.isEmpty() ? View.GONE : View.VISIBLE);
        binding.rvUsedVouchers.setVisibility(usedVouchers.isEmpty() ? View.GONE : View.VISIBLE);

        if (activeVouchers.isEmpty() && usedVouchers.isEmpty()) {
            binding.rvVouchers.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvVouchers.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void observeCoupons() {
        viewModel.getCoupons().observe(getViewLifecycleOwner(), this::syncCouponsToVoucherWallet);
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });
    }

    private void syncCouponsToVoucherWallet(List<CouponDto> coupons) {
        if (coupons == null || coupons.isEmpty() || !isAdded()) {
            return;
        }

        VoucherManager voucherManager = VoucherManager.getInstance(requireContext());
        List<VoucherDto> currentVouchers = voucherManager.getVouchers();
        List<VoucherDto> syncedVouchers = new ArrayList<>();

        for (CouponDto coupon : coupons) {
            VoucherDto converted = VoucherManager.convertCouponToVoucher(requireContext(), coupon);
            if (converted == null) continue;

            VoucherDto existing = findVoucherByCode(currentVouchers, converted.code);
            if (existing != null) {
                converted.isUsed = existing.isUsed;
            }
            syncedVouchers.add(converted);
        }

        voucherManager.saveVouchers(syncedVouchers);
        loadVouchers();
    }

    private VoucherDto findVoucherByCode(List<VoucherDto> vouchers, String code) {
        if (vouchers == null || code == null) return null;
        for (VoucherDto voucher : vouchers) {
            if (voucher != null && code.equalsIgnoreCase(voucher.code)) {
                return voucher;
            }
        }
        return null;
    }

    private VoucherDto findOptimalVoucher(List<VoucherDto> vouchers, VoucherManager voucherManager) {
        VoucherDto bestVoucher = null;
        double bestDiscount = 0;

        for (VoucherDto voucher : vouchers) {
            if (voucher == null) continue;

            double discount = voucherManager.calculateDiscount(voucher, cartSubtotal);
            if (discount <= 0) continue;

            if (bestVoucher == null || discount > bestDiscount) {
                bestVoucher = voucher;
                bestDiscount = discount;
            }
        }

        return bestVoucher;
    }

    private void bindOptimalVoucher(VoucherDto voucher, VoucherManager voucherManager) {
        if (binding == null) return;

        if (voucher == null) {
            binding.layoutOptimalContainer.setVisibility(View.GONE);
            return;
        }

        binding.layoutOptimalContainer.setVisibility(View.VISIBLE);
        binding.tvOptimalCode.setText(voucher.code);
        binding.tvOptimalName.setText(voucher.name);
        binding.tvOptimalDescription.setText(voucher.description);
        binding.tvOptimalExpiry.setText(voucher.expirationDate);

        double savings = voucherManager.calculateDiscount(voucher, cartSubtotal);
        binding.tvOptimalSavings.setText(getString(
                R.string.voucher_discount_amount,
                FormatUtil.formatCurrency(savings)
        ));

        binding.btnOptimalUse.setOnClickListener(v -> {
            voucherManager.setSelectedVoucherCode(voucher.code);
            ToastUtil.show(requireContext(), getString(R.string.toast_voucher_applied, voucher.code));
            if (isApplyFlow) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
