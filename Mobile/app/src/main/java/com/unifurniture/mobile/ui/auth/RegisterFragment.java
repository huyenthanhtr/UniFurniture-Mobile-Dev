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
import com.unifurniture.mobile.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Guests must always be able to return to Home without registering.
        binding.btnBackHome.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) ((AuthActivity) getActivity()).goHome();
        });

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirm = binding.etConfirmPassword.getText().toString().trim();
            viewModel.register(name, phone, email, password, confirm);
        });

        binding.tvLogin.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnRegister.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) com.unifurniture.mobile.util.CustomBlueDialog.showError(requireContext(), error);
        });

        // Register success → OTP was sent. Move to the OTP screen carrying the formatted phone.
        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), formattedPhone -> {
            if (formattedPhone == null) return;
            viewModel.clearRegisterSuccess();
            Toast.makeText(requireContext(),
                    getString(R.string.register_otp_sent), Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, OtpFragment.newInstance(formattedPhone, viewModel.getPendingOtp()))
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
