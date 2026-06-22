package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CustomerAddressDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentAddressBookBinding;
import com.unifurniture.mobile.databinding.ItemCustomerAddressBinding;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class AddressBookFragment extends Fragment {

    private static final String PROVINCE_API_URL = "https://provinces.open-api.vn/api/?depth=1";

    private FragmentAddressBookBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String editingAddressId = null;
    private boolean isLoading = false;
    private ArrayAdapter<String> provinceAdapter;
    private ArrayAdapter<String> districtAdapter;
    private final OkHttpClient geoClient = new OkHttpClient();
    private final Map<String, Integer> provinceCodeByName = new HashMap<>();
    private final List<String> provinceNames = new ArrayList<>();
    private final List<String> districtNames = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddressBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnAddAddress.setOnClickListener(v -> openAddForm());
        binding.btnSaveAddress.setOnClickListener(v -> saveAddress());
        binding.btnCancelEdit.setOnClickListener(v -> closeForm());

        setupLocationDropdowns();
        closeForm();
        loadProvinces();
        loadAddresses();
    }

    private void setupLocationDropdowns() {
        provinceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, provinceNames);
        districtAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, districtNames);

        binding.etProvince.setAdapter(provinceAdapter);
        binding.etDistrict.setAdapter(districtAdapter);

        binding.etProvince.setOnItemClickListener((parent, view, position, id) -> {
            String province = provinceAdapter.getItem(position);
            binding.etProvince.setError(null);
            binding.etDistrict.setText("", false);
            binding.etDistrict.setEnabled(true);
            loadDistrictsByProvinceName(province, "");
        });

        binding.etDistrict.setOnItemClickListener((parent, view, position, id) ->
                binding.etDistrict.setError(null));
    }

    private void loadProvinces() {
        Request request = new Request.Builder().url(PROVINCE_API_URL).build();
        geoClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> setFallbackProvinces());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> setFallbackProvinces());
                    return;
                }

                String raw = response.body().string();
                ProvinceApi[] items = new com.google.gson.Gson().fromJson(raw, ProvinceApi[].class);
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    provinceCodeByName.clear();
                    provinceNames.clear();
                    if (items != null) {
                        for (ProvinceApi item : items) {
                            String name = item != null ? safe(item.name) : "";
                            if (!name.isEmpty()) {
                                provinceCodeByName.put(name, item.code);
                                provinceNames.add(name);
                            }
                        }
                    }
                    if (provinceNames.isEmpty()) {
                        setFallbackProvinces();
                    } else {
                        java.util.Collections.sort(provinceNames, String::compareToIgnoreCase);
                        provinceAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private void setFallbackProvinces() {
        provinceCodeByName.clear();
        provinceNames.clear();
        String[] fallback = new String[] {
                "TP. Ho Chi Minh",
                "Binh Duong",
                "Dong Nai",
                "Long An",
                "Tay Ninh",
                "Ba Ria - Vung Tau",
                "Tien Giang"
        };
        java.util.Collections.addAll(provinceNames, fallback);
        provinceAdapter.notifyDataSetChanged();
    }

    private void loadDistrictsByProvinceName(String provinceName, String preferredDistrict) {
        districtNames.clear();
        districtAdapter.notifyDataSetChanged();
        binding.etDistrict.setEnabled(false);

        Integer code = provinceCodeByName.get(provinceName);
        if (code == null) {
            addFallbackDistrict(preferredDistrict);
            return;
        }

        Request request = new Request.Builder()
                .url("https://provinces.open-api.vn/api/p/" + code + "?depth=2")
                .build();

        geoClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> addFallbackDistrict(preferredDistrict));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> addFallbackDistrict(preferredDistrict));
                    return;
                }

                String raw = response.body().string();
                ProvinceDetailApi detail = new com.google.gson.Gson().fromJson(raw, ProvinceDetailApi.class);
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    districtNames.clear();
                    if (detail != null && detail.districts != null) {
                        for (DistrictApi item : detail.districts) {
                            String name = item != null ? safe(item.name) : "";
                            if (!name.isEmpty()) {
                                districtNames.add(name);
                            }
                        }
                    }

                    if (districtNames.isEmpty()) {
                        addFallbackDistrict(preferredDistrict);
                        return;
                    }

                    java.util.Collections.sort(districtNames, String::compareToIgnoreCase);
                    if (!safe(preferredDistrict).isEmpty() && !districtNames.contains(preferredDistrict)) {
                        districtNames.add(0, preferredDistrict);
                    }
                    districtAdapter.notifyDataSetChanged();
                    binding.etDistrict.setEnabled(true);
                    if (!safe(preferredDistrict).isEmpty()) {
                        binding.etDistrict.setText(preferredDistrict, false);
                    }
                });
            }
        });
    }

    private void addFallbackDistrict(String preferredDistrict) {
        districtNames.clear();
        if (!safe(preferredDistrict).isEmpty()) {
            districtNames.add(preferredDistrict);
        }
        districtNames.add(getString(R.string.district));
        districtAdapter.notifyDataSetChanged();
        binding.etDistrict.setEnabled(true);
        if (!safe(preferredDistrict).isEmpty()) {
            binding.etDistrict.setText(preferredDistrict, false);
        }
    }

    private void openAddForm() {
        clearForm();
        binding.cardForm.setVisibility(View.VISIBLE);
        binding.btnAddAddress.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);
    }

    private void closeForm() {
        clearForm();
        binding.cardForm.setVisibility(View.GONE);
        binding.btnAddAddress.setVisibility(View.VISIBLE);
    }

    private void loadAddresses() {
        if (binding == null || isLoading) return;

        String customerId = sessionManager.getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            binding.containerAddresses.removeAllViews();
            showEmptyState(true, getString(R.string.login_required_orders_hint));
            return;
        }

        isLoading = true;
        binding.containerAddresses.removeAllViews();
        showEmptyState(false, "");

        apiService.getCustomerAddresses(customerId, 100).enqueue(new retrofit2.Callback<ApiListResponse<CustomerAddressDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiListResponse<CustomerAddressDto>> call,
                                   @NonNull Response<ApiListResponse<CustomerAddressDto>> response) {
                isLoading = false;
                if (!isAdded() || binding == null) return;

                List<CustomerAddressDto> items = response.body() != null ? response.body().getData() : null;
                if (response.isSuccessful()) {
                    if (items == null || items.isEmpty()) {
                        binding.containerAddresses.removeAllViews();
                        showEmptyState(true, getString(R.string.address_book_empty));
                    } else {
                        renderAddresses(items);
                        showEmptyState(false, "");
                    }
                } else {
                    binding.containerAddresses.removeAllViews();
                    showEmptyState(true, readErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiListResponse<CustomerAddressDto>> call, @NonNull Throwable t) {
                isLoading = false;
                if (!isAdded() || binding == null) return;
                binding.containerAddresses.removeAllViews();
                showEmptyState(true, getString(R.string.error_network, t.getMessage()));
            }
        });
    }

    private void renderAddresses(List<CustomerAddressDto> items) {
        binding.containerAddresses.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CustomerAddressDto item : items) {
            ItemCustomerAddressBinding itemBinding = ItemCustomerAddressBinding.inflate(inflater, binding.containerAddresses, false);
            itemBinding.tvFullName.setText(safe(item.getFullName()));
            itemBinding.tvPhone.setText(safe(item.getPhone()));
            itemBinding.tvAddress.setText(item.getDisplayAddress());
            itemBinding.tvDefaultBadge.setVisibility(item.isDefault() ? View.VISIBLE : View.GONE);
            itemBinding.btnSetDefault.setVisibility(item.isDefault() ? View.GONE : View.VISIBLE);

            itemBinding.btnEdit.setOnClickListener(v -> populateForm(item));
            itemBinding.btnDelete.setOnClickListener(v -> confirmDelete(item));
            itemBinding.btnSetDefault.setOnClickListener(v -> {
                Map<String, Object> body = toRequestBody(item);
                body.put("is_default", true);
                updateAddress(item.getId(), body, true);
            });

            binding.containerAddresses.addView(itemBinding.getRoot());
        }
    }

    private void populateForm(CustomerAddressDto item) {
        editingAddressId = item.getId();
        binding.cardForm.setVisibility(View.VISIBLE);
        binding.btnAddAddress.setVisibility(View.GONE);
        binding.tvFormTitle.setText(R.string.edit_address);
        binding.etFullName.setText(safe(item.getFullName()));
        binding.etPhone.setText(safe(item.getPhone()));
        binding.etProvince.setText(safe(item.getProvince()), false);
        binding.etDistrict.setEnabled(true);
        binding.etDistrict.setText(safe(item.getDistrict()), false);
        binding.etAddress.setText(safe(item.getAddress()));
        binding.switchDefault.setChecked(item.isDefault());
        loadDistrictsByProvinceName(item.getProvince(), item.getDistrict());
    }

    private void clearForm() {
        editingAddressId = null;
        binding.tvFormTitle.setText(R.string.add_new_address);
        binding.etFullName.setText("");
        binding.etPhone.setText("");
        binding.etProvince.setText("", false);
        binding.etDistrict.setText("", false);
        binding.etDistrict.setEnabled(false);
        binding.etAddress.setText("");
        binding.switchDefault.setChecked(false);
        binding.etFullName.setError(null);
        binding.etPhone.setError(null);
        binding.etProvince.setError(null);
        binding.etDistrict.setError(null);
        binding.etAddress.setError(null);
    }

    private void saveAddress() {
        String customerId = sessionManager.getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            ToastUtil.show(requireContext(), R.string.toast_login_required_addresses);
            return;
        }

        String fullName = textOf(binding.etFullName);
        String phone = textOf(binding.etPhone);
        String province = textOf(binding.etProvince);
        String district = textOf(binding.etDistrict);
        String address = textOf(binding.etAddress);

        boolean valid = validateForm(fullName, phone, province, district, address);
        if (!valid) {
            ToastUtil.show(requireContext(), R.string.fill_required);
            return;
        }

        setFormEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("customer_id", customerId);
        body.put("customer_address_name", fullName);
        body.put("address_phone", phone);
        body.put("province", province);
        body.put("district", district);
        body.put("address_line", address);
        body.put("ward", "");
        body.put("is_default", binding.switchDefault.isChecked());
        body.put("status", "active");

        if (editingAddressId == null || editingAddressId.isEmpty()) {
            apiService.createCustomerAddress(body).enqueue(new SimpleAddressCallback());
        } else {
            updateAddress(editingAddressId, body, false);
        }
    }

    private boolean validateForm(String fullName, String phone, String province, String district, String address) {
        boolean valid = true;
        if (fullName.isEmpty()) {
            binding.etFullName.setError(getString(R.string.required_field));
            valid = false;
        }
        if (phone.isEmpty()) {
            binding.etPhone.setError(getString(R.string.required_field));
            valid = false;
        } else {
            String normalizedPhone = phone.replaceAll("[\\s.-]", "");
            if (!normalizedPhone.matches("^(0\\d{9}|\\+84\\d{9})$")) {
                binding.etPhone.setError(getString(R.string.invalid_phone));
                valid = false;
            }
        }
        if (province.isEmpty()) {
            binding.etProvince.setError(getString(R.string.invalid_province));
            valid = false;
        }
        if (district.isEmpty()) {
            binding.etDistrict.setError(getString(R.string.invalid_district));
            valid = false;
        }
        if (address.isEmpty()) {
            binding.etAddress.setError(getString(R.string.required_field));
            valid = false;
        }
        return valid;
    }

    private void updateAddress(String addressId, Map<String, Object> body, boolean fromDefaultAction) {
        apiService.updateCustomerAddress(addressId, body).enqueue(new retrofit2.Callback<CustomerAddressDto>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<CustomerAddressDto> call, @NonNull Response<CustomerAddressDto> response) {
                setFormEnabled(true);
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful()) {
                    ToastUtil.show(requireContext(), fromDefaultAction ? R.string.default_address : R.string.profile_updated);
                    closeForm();
                    loadAddresses();
                } else {
                    ToastUtil.error(requireContext(), readErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<CustomerAddressDto> call, @NonNull Throwable t) {
                setFormEnabled(true);
                if (isAdded()) {
                    ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
                }
            }
        });
    }

    private void confirmDelete(CustomerAddressDto item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(item.getDisplayAddress())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAddress(item))
                .show();
    }

    private void deleteAddress(CustomerAddressDto item) {
        apiService.deleteCustomerAddress(item.getId()).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful()) {
                    ToastUtil.show(requireContext(), R.string.address_deleted);
                    if (safe(item.getId()).equals(editingAddressId)) {
                        closeForm();
                    }
                    loadAddresses();
                } else {
                    ToastUtil.error(requireContext(), readErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {
                if (isAdded()) {
                    ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
                }
            }
        });
    }

    private Map<String, Object> toRequestBody(CustomerAddressDto item) {
        Map<String, Object> body = new HashMap<>();
        body.put("customer_id", sessionManager.getCustomerId());
        body.put("customer_address_name", item.getFullName());
        body.put("address_phone", item.getPhone());
        body.put("province", item.getProvince());
        body.put("district", item.getDistrict());
        body.put("address_line", item.getAddress());
        body.put("ward", "");
        body.put("is_default", item.isDefault());
        body.put("status", "active");
        return body;
    }

    private void showEmptyState(boolean show, String message) {
        binding.tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setText(message);
    }

    private void setFormEnabled(boolean enabled) {
        binding.btnSaveAddress.setEnabled(enabled);
        binding.btnCancelEdit.setEnabled(enabled);
        binding.switchDefault.setEnabled(enabled);
        binding.etFullName.setEnabled(enabled);
        binding.etPhone.setEnabled(enabled);
        binding.etProvince.setEnabled(enabled);
        binding.etDistrict.setEnabled(enabled && !textOf(binding.etProvince).isEmpty());
        binding.etAddress.setEnabled(enabled);
    }

    private String textOf(TextView view) {
        return view.getText() != null ? view.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String readErrorMessage(Response<?> response) {
        try {
            String raw = response.errorBody() != null ? response.errorBody().string() : "";
            if (raw == null || raw.trim().isEmpty()) return getString(R.string.error_unknown);
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("message")) return json.get("message").getAsString();
            if (json.has("error")) return json.get("error").getAsString();
        } catch (Exception ignored) {
        }
        return getString(R.string.error_unknown);
    }

    private class SimpleAddressCallback implements retrofit2.Callback<CustomerAddressDto> {
        @Override
        public void onResponse(@NonNull retrofit2.Call<CustomerAddressDto> call, @NonNull Response<CustomerAddressDto> response) {
            setFormEnabled(true);
            if (!isAdded() || binding == null) return;

            if (response.isSuccessful()) {
                ToastUtil.show(requireContext(), R.string.profile_updated);
                closeForm();
                loadAddresses();
            } else {
                ToastUtil.error(requireContext(), readErrorMessage(response));
            }
        }

        @Override
        public void onFailure(@NonNull retrofit2.Call<CustomerAddressDto> call, @NonNull Throwable t) {
            setFormEnabled(true);
            if (isAdded()) {
                ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ProvinceApi {
        @SerializedName("code")
        int code;
        @SerializedName("name")
        String name;
    }

    private static class DistrictApi {
        @SerializedName("name")
        String name;
    }

    private static class ProvinceDetailApi {
        @SerializedName("districts")
        List<DistrictApi> districts;
    }
}
