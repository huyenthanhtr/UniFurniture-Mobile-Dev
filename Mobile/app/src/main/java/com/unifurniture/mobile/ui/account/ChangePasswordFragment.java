package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentChangePasswordBinding;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String oldPwd = getText(binding.etOldPassword);
        String newPwd = getText(binding.etNewPassword);
        String confirmPwd = getText(binding.etConfirmPassword);

        binding.tilOldPassword.setError(null);
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
        binding.tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(oldPwd)) {
            binding.tilOldPassword.setError(getString(R.string.required_field));
            return;
        }
        if (TextUtils.isEmpty(newPwd)) {
            binding.tilNewPassword.setError(getString(R.string.required_field));
            return;
        }
        if (newPwd.length() < 6) {
            binding.tilNewPassword.setError(getString(R.string.password_min_six));
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            binding.tilConfirmPassword.setError(getString(R.string.password_confirm_mismatch));
            return;
        }
        if (oldPwd.equals(newPwd)) {
            binding.tilNewPassword.setError(getString(R.string.password_must_be_different));
            return;
        }

        String profileId = sessionManager.getProfileId();
        if (profileId == null || profileId.isEmpty()) {
            ToastUtil.error(requireContext(), R.string.account_not_found_login_again);
            return;
        }

        binding.btnChangePassword.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("old_password", oldPwd);
        body.put("new_password", newPwd);

        apiService.changePassword(profileId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded() || binding == null) return;
                binding.btnChangePassword.setEnabled(true);
                if (response.isSuccessful()) {
                    binding.etOldPassword.setText("");
                    binding.etNewPassword.setText("");
                    binding.etConfirmPassword.setText("");
                    ToastUtil.show(requireContext(), R.string.change_password_success);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                } else {
                    binding.tvError.setText(R.string.current_password_incorrect);
                    binding.tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.btnChangePassword.setEnabled(true);
                binding.tvError.setText(getString(R.string.error_network, t.getMessage()));
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
