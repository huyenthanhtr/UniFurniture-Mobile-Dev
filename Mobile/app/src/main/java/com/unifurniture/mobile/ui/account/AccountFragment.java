package com.unifurniture.mobile.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentAccountBinding;
import com.unifurniture.mobile.ui.auth.AuthActivity;
import com.unifurniture.mobile.util.SessionManager;

import android.widget.Toast;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SessionManager session = SessionManager.getInstance(requireContext());

        if (!session.isLoggedIn()) {
            binding.tvUserName.setText(R.string.guest);
            binding.tvUserPhone.setText(R.string.login_required_orders_hint);
            binding.btnLoginPrompt.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.GONE);
            
            binding.btnLoginPrompt.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AuthActivity.class)));
            
            binding.itemOrders.setOnClickListener(v -> 
                    Toast.makeText(requireContext(), R.string.toast_login_required_orders, Toast.LENGTH_SHORT).show());
        } else {
            binding.btnLoginPrompt.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
            
            var customer = session.getCustomer();
            binding.tvUserName.setText(customer.name != null ? customer.name : getString(R.string.guest_customer));
            binding.tvUserPhone.setText(customer.phone);
            
            binding.itemOrders.setOnClickListener(v -> {
                // Navigate to order list
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, new OrderListFragment())
                        .addToBackStack(null)
                        .commit();
            });
            
            binding.btnLogout.setOnClickListener(v -> {
                session.logout();
                startActivity(new Intent(requireContext(), AuthActivity.class));
                requireActivity().finish();
            });
        }

        binding.itemWishlist.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.wishlistFragment));

        binding.itemNotifications.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.notificationFragment));

        binding.itemVouchers.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("subtotal", 0.0); // open as wallet, see all vouchers
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment, bundle);
        });

        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        if (binding == null) return;
        boolean hasUnread = com.unifurniture.mobile.util.NotificationManager.getInstance(requireContext()).hasUnreadNotifications();
        binding.viewNotificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
