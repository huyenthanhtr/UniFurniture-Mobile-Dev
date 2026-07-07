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
import com.unifurniture.mobile.databinding.FragmentOtpBinding;

public class OtpFragment extends Fragment {

    private static final String ARG_PHONE = "phone";
    private static final String ARG_OTP = "otp";

    private FragmentOtpBinding binding;
    private AuthViewModel viewModel;

    /** Create the OTP screen for an already-formatted (84…) phone from registration. */
    public static OtpFragment newInstance(String formattedPhone) {
        return newInstance(formattedPhone, null);
    }

    /** Same, but pre-fill a demo OTP (server returned it because no SMS provider is configured). */
    public static OtpFragment newInstance(String formattedPhone, String demoOtp) {
        OtpFragment f = new OtpFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE, formattedPhone);
        args.putString(ARG_OTP, demoOtp);
        f.setArguments(args);
        return f;
    }

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

        // Phone comes from the registration step — prefill and lock it.
        String phone = getArguments() != null ? getArguments().getString(ARG_PHONE, "") : "";
        binding.etPhone.setText(phone);
        binding.etPhone.setEnabled(false);

        // Tell the user exactly which number the OTP was sent to (display as a local 0… number).
        if (phone != null && !phone.isEmpty()) {
            String display = phone.startsWith("84") && phone.length() > 2 ? "0" + phone.substring(2) : phone;
            binding.tvSubtitle.setText(getString(R.string.otp_subtitle_format, display));
        }

        // Demo mode: server returned the OTP (no SMS provider), so pre-fill it for a one-tap verify.
        String demoOtp = getArguments() != null ? getArguments().getString(ARG_OTP) : null;
        if (demoOtp != null && !demoOtp.isEmpty()) {
            binding.etOtp.setText(demoOtp);
            binding.tvSubtitle.setText(getString(R.string.otp_subtitle_demo, demoOtp));
        }

        binding.btnVerify.setOnClickListener(v -> {
            String otp = binding.etOtp.getText().toString().trim();
            viewModel.verifyOtp(binding.etPhone.getText().toString().trim(), otp);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                com.unifurniture.mobile.util.CustomBlueDialog.showError(requireContext(), error);
                viewModel.clearError();
            }
        });

        // OTP verified → account created (server returns no token). Like the web, go to Login.
        viewModel.getOtpSuccess().observe(getViewLifecycleOwner(), ok -> {
            if (ok == null || !ok) return;
            viewModel.clearOtpSuccess();
            Toast.makeText(requireContext(),
                    getString(R.string.otp_account_created), Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new LoginFragment())
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
