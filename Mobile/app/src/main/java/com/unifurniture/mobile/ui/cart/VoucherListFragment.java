package com.unifurniture.mobile.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.databinding.FragmentVoucherListBinding;
import com.unifurniture.mobile.data.model.VoucherDto;
import com.unifurniture.mobile.ui.adapter.VoucherAdapter;
import com.unifurniture.mobile.util.VoucherManager;
import com.unifurniture.mobile.R;
import java.util.ArrayList;
import java.util.List;

public class VoucherListFragment extends Fragment {

    private FragmentVoucherListBinding binding;
    private VoucherAdapter adapter;
    private double cartSubtotal = 0;

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
        }

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        setupRecyclerView();
        loadVouchers();
    }

    private void setupRecyclerView() {
        adapter = new VoucherAdapter(cartSubtotal, voucher -> {
            VoucherManager.getInstance(requireContext()).setSelectedVoucherCode(voucher.code);
            if (cartSubtotal >= voucher.minOrderValue) {
                Toast.makeText(requireContext(), getString(R.string.toast_voucher_applied, voucher.code), Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            } else {
                Toast.makeText(requireContext(), R.string.toast_voucher_saved_upsell, Toast.LENGTH_LONG).show();
                
                // Redirect back to HomeFragment to shop
                androidx.navigation.Navigation.findNavController(requireView())
                        .popBackStack(R.id.homeFragment, false);
            }
        });
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVouchers.setAdapter(adapter);
    }

    private void loadVouchers() {
        if (binding == null) return;

        List<VoucherDto> allVouchers = VoucherManager.getInstance(requireContext()).getVouchers();
        List<VoucherDto> activeVouchers = new ArrayList<>();

        for (VoucherDto v : allVouchers) {
            if (!v.isUsed) {
                activeVouchers.add(v);
            }
        }

        adapter.submitList(activeVouchers);

        if (activeVouchers.isEmpty()) {
            binding.rvVouchers.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvVouchers.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
