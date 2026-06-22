package com.unifurniture.mobile.ui.account;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CustomerDto;
import com.unifurniture.mobile.data.model.ProfileDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentEditProfileBinding;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String currentProfileId = null;
    private String pendingAvatarDataUrl = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        initImagePicker();
        return binding.getRoot();
    }

    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null || binding == null) {
                        return;
                    }

                    pendingAvatarDataUrl = buildImageDataUrl(uri);
                    if (pendingAvatarDataUrl == null || pendingAvatarDataUrl.isEmpty()) {
                        ToastUtil.error(requireContext(), R.string.error_unknown);
                        return;
                    }

                    renderAvatar(uri);
                    ToastUtil.show(requireContext(), R.string.avatar_selected_hint);
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());

        setupToolbar();
        setupGenderDropdown();
        setupBirthdayPicker();

        binding.btnChangeAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.ivAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        applyDefaultAvatarState();
        loadProfileData();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigateUp());
    }

    private void setupGenderDropdown() {
        String[] genders = {getString(R.string.male), getString(R.string.female), getString(R.string.other)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, genders);
        binding.etGender.setAdapter(adapter);
    }

    private void setupBirthdayPicker() {
        binding.etBirthday.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String currentDisplayDate = textOf(binding.etBirthday);
            if (!currentDisplayDate.isEmpty()) {
                try {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date date = displayFormat.parse(currentDisplayDate);
                    if (date != null) {
                        calendar.setTime(date);
                    }
                } catch (ParseException ignored) {
                }
            }

            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (picker, year, month, dayOfMonth) -> {
                        String displayDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        binding.etBirthday.setText(displayDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void loadProfileData() {
        CustomerDto customer = sessionManager.getCustomer();
        if (customer == null) {
            return;
        }

        binding.etName.setText(customer.getName());
        binding.etEmail.setText(customer.getEmail());
        binding.etPhone.setText(customer.getPhone());

        String profileId = resolveProfileId(customer);
        if (profileId == null || profileId.isEmpty()) {
            return;
        }

        currentProfileId = profileId;
        sessionManager.saveProfileId(profileId);

        apiService.getProfileById(profileId).enqueue(new Callback<ProfileDto>() {
            @Override
            public void onResponse(Call<ProfileDto> call, Response<ProfileDto> response) {
                if (!isAdded() || binding == null || !response.isSuccessful() || response.body() == null) {
                    return;
                }

                ProfileDto profile = response.body();
                currentProfileId = profile.getId();
                sessionManager.saveProfileId(currentProfileId);

                if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                    renderAvatar(profile.getAvatarUrl());
                } else {
                    applyDefaultAvatarState();
                }

                if (profile.getGender() != null) {
                    binding.etGender.setText(mapGenderValueToLocal(profile.getGender()), false);
                }
                if (profile.getBirthday() != null) {
                    binding.etBirthday.setText(formatBirthdayForDisplay(profile.getBirthday()));
                }
                if (profile.getName() != null && !profile.getName().isEmpty()) {
                    binding.etName.setText(profile.getName());
                }
                if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                    binding.etEmail.setText(profile.getEmail());
                }
            }

            @Override
            public void onFailure(Call<ProfileDto> call, Throwable t) {
                // Use session values when profile fetch fails.
            }
        });
    }

    private void saveProfile() {
        if (currentProfileId == null || currentProfileId.isEmpty()) {
            ToastUtil.error(requireContext(), R.string.error_unknown);
            return;
        }

        String name = textOf(binding.etName);
        String email = textOf(binding.etEmail);
        String gender = mapLocalGenderToValue(textOf(binding.etGender));
        String birthday = formatBirthdayForBackend(textOf(binding.etBirthday));

        if (name.isEmpty()) {
            binding.etName.setError(getString(R.string.error_fill_info));
            return;
        }

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText(R.string.loading);

        Map<String, String> body = new HashMap<>();
        body.put("full_name", name);
        body.put("email", email);
        if (!gender.isEmpty()) {
            body.put("gender", gender);
        }
        if (!birthday.isEmpty()) {
            body.put("date_of_birth", birthday);
        }

        apiService.updateProfile(currentProfileId, body).enqueue(new Callback<ProfileDto>() {
            @Override
            public void onResponse(Call<ProfileDto> call, Response<ProfileDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    restoreSaveButton();
                    if (isAdded()) {
                        ToastUtil.error(requireContext(), readErrorMessage(response));
                    }
                    return;
                }

                handleProfileSaved(response.body(), name, email);
            }

            @Override
            public void onFailure(Call<ProfileDto> call, Throwable t) {
                restoreSaveButton();
                if (isAdded()) {
                    ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
                }
            }
        });
    }

    private void handleProfileSaved(ProfileDto updatedProfile, String fallbackName, String fallbackEmail) {
        currentProfileId = updatedProfile.getId();
        sessionManager.saveProfileId(currentProfileId);

        if (pendingAvatarDataUrl != null && !pendingAvatarDataUrl.isEmpty()) {
            Map<String, String> avatarBody = new HashMap<>();
            avatarBody.put("dataUrl", pendingAvatarDataUrl);

            apiService.uploadProfileAvatar(currentProfileId, avatarBody).enqueue(new Callback<ProfileDto>() {
                @Override
                public void onResponse(Call<ProfileDto> call, Response<ProfileDto> response) {
                    restoreSaveButton();
                    if (response.isSuccessful() && response.body() != null) {
                        pendingAvatarDataUrl = null;
                        finishSave(response.body(), fallbackName, fallbackEmail);
                    } else if (isAdded()) {
                        ToastUtil.error(requireContext(), readErrorMessage(response));
                    }
                }

                @Override
                public void onFailure(Call<ProfileDto> call, Throwable t) {
                    restoreSaveButton();
                    if (isAdded()) {
                        ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
                    }
                }
            });
            return;
        }

        restoreSaveButton();
        finishSave(updatedProfile, fallbackName, fallbackEmail);
    }

    private void finishSave(ProfileDto profile, String fallbackName, String fallbackEmail) {
        currentProfileId = profile.getId();
        sessionManager.saveProfileId(currentProfileId);

        CustomerDto customer = sessionManager.getCustomer();
        if (customer != null) {
            customer.setName(profile.getName() != null ? profile.getName() : fallbackName);
            customer.setEmail(profile.getEmail() != null ? profile.getEmail() : fallbackEmail);
            sessionManager.saveCustomer(customer);
        }

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
            renderAvatar(profile.getAvatarUrl());
        } else {
            applyDefaultAvatarState();
        }

        if (isAdded()) {
            ToastUtil.show(requireContext(), R.string.profile_updated);
            androidx.navigation.Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void renderAvatar(Object source) {
        if (!isAdded() || binding == null) {
            return;
        }

        binding.ivAvatar.setPadding(0, 0, 0, 0);
        binding.ivAvatar.setImageTintList(null);
        binding.ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this)
                .load(source)
                .placeholder(R.drawable.ic_account)
                .error(R.drawable.ic_account)
                .circleCrop()
                .into(binding.ivAvatar);
    }

    private void applyDefaultAvatarState() {
        if (!isAdded() || binding == null) {
            return;
        }

        int padding = dpToPx(24);
        binding.ivAvatar.setImageResource(R.drawable.ic_account);
        binding.ivAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        binding.ivAvatar.setPadding(padding, padding, padding, padding);
        binding.ivAvatar.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
        binding.ivAvatar.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
    }

    private String buildImageDataUrl(Uri uri) {
        if (!isAdded()) {
            return null;
        }

        String mimeType = requireContext().getContentResolver().getType(uri);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg";
        }

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                return null;
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            String base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
            return "data:" + mimeType + ";base64," + base64;
        } catch (IOException e) {
            return null;
        }
    }

    private void restoreSaveButton() {
        if (!isAdded() || binding == null) {
            return;
        }
        binding.btnSave.setEnabled(true);
        binding.btnSave.setText(R.string.save_changes);
    }

    private String formatBirthdayForDisplay(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return "";
        }

        String cleanedDate = rawDate.split("T")[0].split(" ")[0];
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(cleanedDate);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (ParseException ignored) {
            return cleanedDate.length() >= 10 ? cleanedDate.substring(0, 10) : cleanedDate;
        }
        return cleanedDate;
    }

    private String formatBirthdayForBackend(String displayDate) {
        if (displayDate == null || displayDate.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(displayDate);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (ParseException ignored) {
        }
        return displayDate;
    }

    private String resolveProfileId(CustomerDto customer) {
        String profileId = sessionManager.getProfileId();
        if (profileId != null && !profileId.isEmpty()) {
            return profileId;
        }

        String token = sessionManager.getToken();
        if (token != null && !token.isEmpty()) {
            return token;
        }

        return customer != null ? customer.getId() : null;
    }

    private String mapGenderValueToLocal(String gender) {
        if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("nam")) {
            return getString(R.string.male);
        }
        if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("nu")) {
            return getString(R.string.female);
        }
        return getString(R.string.other);
    }

    private String mapLocalGenderToValue(String localGender) {
        if (localGender.equals(getString(R.string.male))) {
            return "male";
        }
        if (localGender.equals(getString(R.string.female))) {
            return "female";
        }
        if (localGender.equals(getString(R.string.other))) {
            return "other";
        }
        return "";
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private String textOf(android.widget.TextView view) {
        return view.getText() != null ? view.getText().toString().trim() : "";
    }

    private String readErrorMessage(Response<?> response) {
        try {
            String raw = response.errorBody() != null ? response.errorBody().string() : "";
            if (raw == null || raw.trim().isEmpty()) {
                return getString(R.string.error_unknown);
            }

            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("message")) {
                return json.get("message").getAsString();
            }
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (Exception ignored) {
        }
        return getString(R.string.error_unknown);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
