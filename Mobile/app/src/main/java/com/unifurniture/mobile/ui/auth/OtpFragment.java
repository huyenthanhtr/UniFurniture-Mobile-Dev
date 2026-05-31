package com.unifurniture.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.unifurniture.mobile.databinding.FragmentOtpBinding;
import com.unifurniture.mobile.ui.MainActivity;
import com.unifurniture.mobile.util.SessionManager;

public class OtpFragment extends Fragment {

    private FragmentOtpBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Pre-fill phone from the pending registration
        viewModel.getPendingPhone().observe(getViewLifecycleOwner(), phone -> {
            if (phone != null && binding.etPhone.getText().toString().trim().isEmpty()) {
                binding.etPhone.setText(phone);
            }
        });

        binding.btnVerify.setOnClickListener(v -> {
            String phone = binding.etPhone.getText().toString().trim();
            String otp = binding.etOtp.getText().toString().trim();
            viewModel.verifyOtp(phone, otp);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.profile != null) {
                // OTP verified — save profile to session and go to main
                SessionManager session = SessionManager.getInstance(requireContext());
                session.saveProfile(result.profile);
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
