package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CustomerAddressDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentAddressBookBinding;
import com.unifurniture.mobile.databinding.ItemCustomerAddressBinding;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.ScrollStateHelper;
import com.unifurniture.mobile.util.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressBookFragment extends Fragment {

    private FragmentAddressBookBinding binding;
    private AddressBookViewModel viewModel;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String editingAddressId = null;
    private final ScrollStateHelper scrollState = new ScrollStateHelper("address_book");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddressBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.addressBookFragment, AddressBookViewModel.class);
        scrollState.read(savedInstanceState);
        apiService = ApiClient.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnSaveAddress.setOnClickListener(v -> saveAddress());
        binding.btnCancelEdit.setOnClickListener(v -> clearForm());

        viewModel.getAddresses().observe(getViewLifecycleOwner(), items -> {
            if (items == null) return;
            if (items.isEmpty()) {
                showEmptyState(true, getString(R.string.address_book_empty));
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                renderAddresses(items);
                scrollState.restore(binding.scrollContent);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            // Only show blocking state on first load
            if (Boolean.TRUE.equals(loading) && viewModel.getAddresses().getValue() == null) {
                binding.tvEmpty.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && isAdded()) {
                showEmptyState(true, getString(R.string.error_network, message));
            }
        });

        String customerId = sessionManager.getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            showEmptyState(true, getString(R.string.login_required_orders_hint));
        } else {
            viewModel.loadAddressesIfNeeded();
        }
    }

    private void renderAddresses(List<CustomerAddressDto> items) {
        binding.containerAddresses.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CustomerAddressDto item : items) {
            ItemCustomerAddressBinding itemBinding = ItemCustomerAddressBinding.inflate(inflater, binding.containerAddresses, false);
            itemBinding.tvFullName.setText(item.fullName);
            itemBinding.tvPhone.setText(item.phone);
            itemBinding.tvAddress.setText(item.toString());
            itemBinding.tvDefaultBadge.setVisibility(item.isDefault ? View.VISIBLE : View.GONE);
            itemBinding.btnSetDefault.setVisibility(item.isDefault ? View.GONE : View.VISIBLE);

            itemBinding.btnEdit.setOnClickListener(v -> populateForm(item));
            itemBinding.btnDelete.setOnClickListener(v -> deleteAddress(item));
            itemBinding.btnSetDefault.setOnClickListener(v -> {
                Map<String, Object> body = toRequestBody(item);
                body.put("is_default", true);
                updateAddress(item.id, body, true);
            });

            binding.containerAddresses.addView(itemBinding.getRoot());
        }
    }

    private void populateForm(CustomerAddressDto item) {
        editingAddressId = item.id;
        binding.tvFormTitle.setText(R.string.edit_address);
        binding.etFullName.setText(item.fullName);
        binding.etPhone.setText(item.phone);
        binding.etProvince.setText(item.province);
        binding.etDistrict.setText(item.district);
        binding.etAddress.setText(item.address);
        binding.switchDefault.setChecked(item.isDefault);
    }

    private void clearForm() {
        editingAddressId = null;
        binding.tvFormTitle.setText(R.string.add_new_address);
        binding.etFullName.setText("");
        binding.etPhone.setText("");
        binding.etProvince.setText("");
        binding.etDistrict.setText("");
        binding.etAddress.setText("");
        binding.switchDefault.setChecked(false);
    }

    private void saveAddress() {
        String fullName = textOf(binding.etFullName);
        String phone = textOf(binding.etPhone);
        String province = textOf(binding.etProvince);
        String district = textOf(binding.etDistrict);
        String address = textOf(binding.etAddress);

        if (fullName.isEmpty() || phone.isEmpty() || province.isEmpty() || district.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), R.string.fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("customer_id", sessionManager.getCustomerId());
        body.put("customer_address_name", fullName);
        body.put("address_phone", phone);
        body.put("province", province);
        body.put("district", district);
        body.put("address_line", address);
        body.put("is_default", binding.switchDefault.isChecked());

        if (editingAddressId == null) {
            apiService.createCustomerAddress(body).enqueue(new SimpleAddressCallback());
        } else {
            updateAddress(editingAddressId, body, false);
        }
    }

    private void updateAddress(String addressId, Map<String, Object> body, boolean fromDefaultAction) {
        apiService.updateCustomerAddress(addressId, body).enqueue(new Callback<CustomerAddressDto>() {
            @Override
            public void onResponse(@NonNull Call<CustomerAddressDto> call, @NonNull Response<CustomerAddressDto> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    if (fromDefaultAction) {
                        Toast.makeText(requireContext(), R.string.default_address, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    }
                    clearForm();
                    viewModel.reloadAddresses();
                } else {
                    Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CustomerAddressDto> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.error_network, t.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteAddress(CustomerAddressDto item) {
        apiService.deleteCustomerAddress(item.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (isAdded() && response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.address_deleted, Toast.LENGTH_SHORT).show();
                    viewModel.reloadAddresses();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.error_network, t.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Map<String, Object> toRequestBody(CustomerAddressDto item) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer_id", sessionManager.getCustomerId());
        body.put("customer_address_name", item.fullName);
        body.put("address_phone", item.phone);
        body.put("province", item.province);
        body.put("district", item.district);
        body.put("address_line", item.address);
        return body;
    }

    private void showEmptyState(boolean show, String message) {
        binding.tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setText(message);
    }

    private String textOf(androidx.appcompat.widget.AppCompatEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private class SimpleAddressCallback implements Callback<CustomerAddressDto> {
        @Override
        public void onResponse(@NonNull Call<CustomerAddressDto> call, @NonNull Response<CustomerAddressDto> response) {
            if (!isAdded()) return;
            if (response.isSuccessful()) {
                Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                clearForm();
                viewModel.reloadAddresses();
            } else {
                Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<CustomerAddressDto> call, @NonNull Throwable t) {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.error_network, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null) scrollState.saveScroll(binding.scrollContent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            scrollState.save(outState, binding.scrollContent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
