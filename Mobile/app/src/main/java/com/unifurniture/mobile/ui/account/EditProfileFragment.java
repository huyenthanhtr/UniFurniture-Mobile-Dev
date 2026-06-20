package com.unifurniture.mobile.ui.account;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CustomerDto;
import com.unifurniture.mobile.data.model.ProfileDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentEditProfileBinding;
import com.unifurniture.mobile.util.SessionManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String currentProfileId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());

        setupToolbar();
        setupGenderDropdown();
        setupBirthdayPicker();
        
        loadProfileData();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v).navigateUp();
        });
    }

    private void setupGenderDropdown() {
        String[] genders = {getString(R.string.male), getString(R.string.female), getString(R.string.other)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, genders);
        binding.etGender.setAdapter(adapter);
    }

    private void setupBirthdayPicker() {
        binding.etBirthday.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year1, month1, dayOfMonth) -> {
                String date = String.format("%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
                binding.etBirthday.setText(date);
            }, year, month, day);
            dialog.show();
        });
    }

    private void loadProfileData() {
        CustomerDto customer = sessionManager.getCustomer();
        if (customer == null) return;

        // Populate basic customer info first
        binding.etName.setText(customer.getName());
        binding.etEmail.setText(customer.getEmail());
        binding.etPhone.setText(customer.getPhone());

        // Fetch profile
        apiService.getProfile(customer.getId()).enqueue(new Callback<ApiListResponse<ProfileDto>>() {
            @Override
            public void onResponse(Call<ApiListResponse<ProfileDto>> call, Response<ApiListResponse<ProfileDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    ProfileDto profile = response.body().getData().get(0);
                    currentProfileId = profile.getId();
                    if (profile.getGender() != null) {
                        String genderStr = mapGenderValueToLocal(profile.getGender());
                        binding.etGender.setText(genderStr, false);
                    }
                    if (profile.getBirthday() != null) {
                        binding.etBirthday.setText(profile.getBirthday());
                    }
                    // Overwrite with profile name/email if they exist
                    if (profile.getName() != null && !profile.getName().isEmpty()) {
                        binding.etName.setText(profile.getName());
                    }
                    if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                        binding.etEmail.setText(profile.getEmail());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiListResponse<ProfileDto>> call, Throwable t) {
                // Ignore, we just use CustomerDto data
            }
        });
    }

    private String mapGenderValueToLocal(String gender) {
        if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("Male") || gender.equalsIgnoreCase("Nam")) return getString(R.string.male);
        if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("Female") || gender.equalsIgnoreCase("Nữ")) return getString(R.string.female);
        return getString(R.string.other);
    }
    
    private String mapLocalGenderToValue(String localGender) {
        if (localGender.equals(getString(R.string.male))) return "male";
        if (localGender.equals(getString(R.string.female))) return "female";
        return "other";
    }

    private void saveProfile() {
        if (currentProfileId == null) {
            Toast.makeText(requireContext(), getString(R.string.error_unknown), Toast.LENGTH_SHORT).show();
            return;
        }

        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String gender = mapLocalGenderToValue(binding.etGender.getText().toString().trim());
        String birthday = binding.etBirthday.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etName.setError(getString(R.string.error_fill_info));
            return;
        }

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText(R.string.loading);

        Map<String, String> body = new HashMap<>();
        body.put("full_name", name);
        body.put("email", email);
        if (!gender.isEmpty()) body.put("gender", gender);
        if (!birthday.isEmpty()) body.put("date_of_birth", birthday);

        apiService.updateProfile(currentProfileId, body).enqueue(new Callback<ProfileDto>() {
            @Override
            public void onResponse(Call<ProfileDto> call, Response<ProfileDto> response) {
                if (isAdded()) {
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText(R.string.save_changes);
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    // Update session customer data
                    CustomerDto customer = sessionManager.getCustomer();
                    if (customer != null) {
                        customer.setName(name);
                        customer.setEmail(email);
                        sessionManager.saveCustomer(customer);
                    }
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                        androidx.navigation.Navigation.findNavController(requireView()).navigateUp();
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), getString(R.string.error_unknown), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ProfileDto> call, Throwable t) {
                if (isAdded()) {
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText(R.string.save_changes);
                    Toast.makeText(requireContext(), getString(R.string.error_network, t.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
