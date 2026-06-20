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
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentLoginBinding;
import com.unifurniture.mobile.ui.MainActivity;
import com.unifurniture.mobile.util.SessionManager;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Sec 6: Guests must always be able to return to Home without logging in.
        binding.btnBackHome.setOnClickListener(v -> goHome());

        // Sec 5: pre-fill remembered credentials.
        com.unifurniture.mobile.util.CredentialStore creds =
                com.unifurniture.mobile.util.CredentialStore.getInstance(requireContext());
        if (creds.isRemembered()) {
            binding.cbRememberPassword.setChecked(true);
            binding.etPhone.setText(creds.getPhone());
            binding.etPassword.setText(creds.getPassword());
        }

        binding.btnLogin.setOnClickListener(v -> {
            String phone = binding.etPhone.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            viewModel.login(phone, password);
        });

        binding.tvRegister.setOnClickListener(v -> {
            // Navigate to register
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Observers
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnLogin.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.token != null) {
                SessionManager session = SessionManager.getInstance(requireContext());
                session.saveToken(result.token);
                session.saveCustomer(result.customer);

                // Sec 5: persist or forget credentials based on the checkbox.
                if (binding.cbRememberPassword.isChecked()) {
                    creds.save(binding.etPhone.getText().toString().trim(),
                            binding.etPassword.getText().toString().trim());
                } else {
                    creds.clear();
                }

                // Merge local wishlist to server
                java.util.Set<String> localWishlist = session.getLocalWishlist();
                if (!localWishlist.isEmpty() && result.customer != null) {
                    com.unifurniture.mobile.data.repository.ProductRepository productRepo = 
                        new com.unifurniture.mobile.data.repository.ProductRepository(
                            com.unifurniture.mobile.UniFurnitureApp.getInstance().getApiService());
                    for (String productId : localWishlist) {
                        productRepo.addToWishlist(result.customer.id, productId);
                    }
                    session.clearLocalWishlist();
                }

                // Add welcome notification
                com.unifurniture.mobile.util.NotificationManager.getInstance(requireContext())
                        .addNotification(
                                getString(R.string.login_welcome),
                                getString(R.string.login_success_msg),
                                "account",
                                null
                        );

                // Go to MainActivity
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            }
        });
    }

    /**
     * Sec 6: Return the guest to the shopping experience (Home). AuthActivity sits on top of
     * MainActivity, so finishing it drops the user straight back where they were browsing.
     */
    private void goHome() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
