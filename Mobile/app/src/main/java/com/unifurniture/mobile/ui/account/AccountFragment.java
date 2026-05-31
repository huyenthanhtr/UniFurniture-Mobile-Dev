package com.unifurniture.mobile.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.unifurniture.mobile.data.model.ProfileDto;
import com.unifurniture.mobile.databinding.FragmentAccountBinding;
import com.unifurniture.mobile.ui.auth.AuthActivity;
import com.unifurniture.mobile.util.SessionManager;

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
            binding.layoutGuest.setVisibility(View.VISIBLE);
            binding.layoutUser.setVisibility(View.GONE);
            binding.btnLoginPrompt.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AuthActivity.class)));
            return;
        }

        binding.layoutGuest.setVisibility(View.GONE);
        binding.layoutUser.setVisibility(View.VISIBLE);

        ProfileDto profile = session.getProfile();
        binding.tvUserName.setText(profile.getDisplayName());
        binding.tvUserPhone.setText(profile.phone != null ? profile.phone : "");

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
