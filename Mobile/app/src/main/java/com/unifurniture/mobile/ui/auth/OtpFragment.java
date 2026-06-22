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

    private FragmentOtpBinding binding;
    private AuthViewModel viewModel;

    /** Create the OTP screen for an already-formatted (84…) phone from registration. */
    public static OtpFragment newInstance(String formattedPhone) {
        OtpFragment f = new OtpFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE, formattedPhone);
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

        binding.btnVerify.setOnClickListener(v -> {
            String otp = binding.etOtp.getText().toString().trim();
            viewModel.verifyOtp(binding.etPhone.getText().toString().trim(), otp);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
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
