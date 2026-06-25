package com.unifurniture.mobile.ui.checkout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.NavOptions;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentOrderSuccessBinding;

public class OrderSuccessFragment extends Fragment {

    private FragmentOrderSuccessBinding binding;
    private String orderId;
    private String orderCode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            orderId = getArguments().getString("order_id");
            orderCode = getArguments().getString("order_code");
        }

        binding.tvOrderCode.setText(orderCode != null ? orderCode : "UF-XXXXXX");

        if (orderId == null || orderId.trim().isEmpty()) {
            binding.btnViewOrderDetail.setVisibility(View.GONE);
        } else {
            binding.btnViewOrderDetail.setVisibility(View.VISIBLE);
            binding.btnViewOrderDetail.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("order_id", orderId);
                Navigation.findNavController(requireView()).navigate(R.id.orderDetailFragment, bundle);
            });
        }

        binding.btnGoHome.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, true)
                    .build();
            Navigation.findNavController(requireView()).navigate(R.id.homeFragment, null, navOptions);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
