package com.unifurniture.mobile.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentForgotBinding;

public class ForgotFragment extends Fragment {

    private FragmentForgotBinding binding;
    private AuthViewModel viewModel;

    private String phoneNum = "";
    private String otpCode = "";
    private String currentStep = "phone"; // "phone", "otp", "newpass"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (savedInstanceState != null) {
            currentStep = savedInstanceState.getString("step", "phone");
            phoneNum = savedInstanceState.getString("phone_num", "");
            otpCode = savedInstanceState.getString("otp_code", "");
        }

        updateStepVisibility();

        // Guests must always be able to return to Home without resetting a password.
        binding.btnBackHome.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) ((AuthActivity) getActivity()).goHome();
        });

        binding.btnSendOtp.setOnClickListener(v -> {
            String phone = binding.etForgotPhone.getText().toString().trim();
            if (phone.length() < 9 || phone.length() > 10) {
                Toast.makeText(requireContext(), R.string.str_invalid_phone, Toast.LENGTH_SHORT).show();
                return;
            }
            phoneNum = phone;
            viewModel.forgotPassword(phone);
        });

        binding.btnVerifyForgotOtp.setOnClickListener(v -> {
            String otp = binding.etForgotOtp.getText().toString().trim();
            if (otp.length() != 6) {
                Toast.makeText(requireContext(), R.string.str_invalid_otp, Toast.LENGTH_SHORT).show();
                return;
            }
            otpCode = otp;
            currentStep = "newpass";
            updateStepVisibility();
        });

        binding.btnResetPassword.setOnClickListener(v -> {
            String newPassword = binding.etForgotNewPassword.getText().toString().trim();
            String confirmPassword = binding.etForgotConfirmPassword.getText().toString().trim();

            if (newPassword.length() < 8) {
                Toast.makeText(requireContext(), R.string.str_password_too_short, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(requireContext(), R.string.str_password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.resetPassword(phoneNum, otpCode, newPassword);
        });

        binding.tvBackToLogin.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Observers
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnSendOtp.setEnabled(!loading);
            binding.btnVerifyForgotOtp.setEnabled(!loading);
            binding.btnResetPassword.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                com.unifurniture.mobile.util.CustomBlueDialog.showError(requireContext(), error);
                viewModel.clearError();
            }
        });

        viewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (currentStep.equals("phone")) {
                    currentStep = "otp";
                    updateStepVisibility();
                    binding.tvOtpMessage.setText(getString(R.string.str_forgot_otp_instruction_format, phoneNum));
                    // Demo mode: server returned the OTP (no SMS provider) → pre-fill it.
                    if (result.otp != null && !result.otp.isEmpty()) {
                        binding.etForgotOtp.setText(result.otp);
                    }
                } else if (currentStep.equals("newpass")) {
                    Toast.makeText(requireContext(), result.message != null ? result.message : getString(R.string.str_reset_password_success), Toast.LENGTH_LONG).show();
                    // Go back to login screen
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void updateStepVisibility() {
        binding.layoutPhone.setVisibility(currentStep.equals("phone") ? View.VISIBLE : View.GONE);
        binding.layoutOtp.setVisibility(currentStep.equals("otp") ? View.VISIBLE : View.GONE);
        binding.layoutNewPass.setVisibility(currentStep.equals("newpass") ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("step", currentStep);
        outState.putString("phone_num", phoneNum);
        outState.putString("otp_code", otpCode);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
