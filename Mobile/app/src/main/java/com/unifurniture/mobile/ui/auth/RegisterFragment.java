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

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            viewModel.register(phone, password, name);
        });

        binding.tvLogin.setOnClickListener(v -> requireActivity().onBackPressed());

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnRegister.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Toast.makeText(requireContext(),
                        getString(R.string.register_success_otp), Toast.LENGTH_LONG).show();
                // Navigate to OTP screen
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, new OtpFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
