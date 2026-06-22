package com.unifurniture.mobile.ui.account;

import android.content.Intent;
import android.util.TypedValue;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.ProfileDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentAccountBinding;
import com.unifurniture.mobile.ui.auth.AuthActivity;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;
import com.bumptech.glide.Glide;

import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        SessionManager session = SessionManager.getInstance(requireContext());

        if (!session.isLoggedIn()) {
            binding.tvUserName.setText(R.string.guest);
            binding.tvUserPhone.setText(R.string.login_required_orders_hint);
            binding.tvUserEmail.setVisibility(View.GONE);
            binding.btnEditProfile.setVisibility(View.GONE);
            binding.btnLoginPrompt.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.GONE);
            
            binding.btnLoginPrompt.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AuthActivity.class)));
            
            binding.itemOrders.setOnClickListener(v -> 
                    ToastUtil.show(requireContext(), R.string.toast_login_required_orders));
            binding.itemMyReviews.setOnClickListener(v -> 
                    ToastUtil.show(requireContext(), R.string.toast_login_required_reviews));
            binding.itemAddresses.setOnClickListener(v ->
                    ToastUtil.show(requireContext(), R.string.toast_login_required_addresses));
            binding.itemChangePassword.setOnClickListener(v ->
                    ToastUtil.show(requireContext(), R.string.toast_login_required_password));
            binding.itemNotifications.setOnClickListener(v ->
                    ToastUtil.show(requireContext(), R.string.toast_login_required_notifications));
            binding.itemVouchers.setOnClickListener(v ->
                    ToastUtil.show(requireContext(), R.string.toast_login_required_vouchers));
        } else {
            binding.btnLoginPrompt.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
            binding.btnEditProfile.setVisibility(View.VISIBLE);
            
            var customer = session.getCustomer();
            binding.tvUserName.setText(customer.getName() != null ? customer.getName() : getString(R.string.guest_customer));
            binding.tvUserPhone.setText(customer.getPhone());
            
            if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                binding.tvUserEmail.setText(customer.getEmail());
                binding.tvUserEmail.setVisibility(View.VISIBLE);
            } else {
                binding.tvUserEmail.setVisibility(View.GONE);
            }
            
            binding.btnEditProfile.setOnClickListener(v -> {
                // Navigate to edit profile
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.profileFragment);
            });
            
            binding.itemOrders.setOnClickListener(v -> 
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.orderListFragment));

            binding.itemAddresses.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.addressBookFragment));
            
            binding.itemMyReviews.setOnClickListener(v -> {
                // Navigate to my reviews
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.myReviewsFragment);
            });

            binding.itemChangePassword.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.changePasswordFragment);
            });
            
            binding.btnLogout.setOnClickListener(v -> {
                session.logout();
                startActivity(new Intent(requireContext(), AuthActivity.class));
                requireActivity().finish();
            });
        }

        binding.itemWishlist.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.wishlistFragment));

        binding.itemNotifications.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.notificationFragment));

        binding.itemVouchers.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("subtotal", 0.0); // open as wallet, see all vouchers
            bundle.putString("entry_mode", "browse");
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment, bundle);
        });

        binding.itemOrderTracking.setOnClickListener(v -> 
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.orderTrackingFragment));

        String baseUrl = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");

        binding.itemAbout.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("title", getString(R.string.account_about_title));
            bundle.putString("url", baseUrl + "/ve-unifurniture");
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
        });

        binding.cardPolicySales.setOnClickListener(v -> openPolicy(baseUrl, "chinh-sach-ban-hang", getString(R.string.policy_sales)));
        binding.cardPolicyShipping.setOnClickListener(v -> openPolicy(baseUrl, "giao-hang-lap-dat", getString(R.string.policy_shipping)));
        binding.cardPolicyWarranty.setOnClickListener(v -> openPolicy(baseUrl, "bao-hanh-bao-tri", getString(R.string.policy_warranty)));
        binding.cardPolicyReturn.setOnClickListener(v -> openPolicy(baseUrl, "doi-tra", getString(R.string.policy_return)));
        binding.cardPolicyLoyalty.setOnClickListener(v -> openPolicy(baseUrl, "khach-hang-than-thiet", getString(R.string.policy_loyalty)));
        binding.cardPolicyPartner.setOnClickListener(v -> openPolicy(baseUrl, "doi-tac-ban-hang", getString(R.string.policy_partner)));

        binding.itemLanguage.setOnClickListener(v ->
                com.unifurniture.mobile.util.LanguageDialog.show(requireContext(), false, (code, changed) -> {
                    // Sec 2: apply immediately by recreating the activity so the whole UI refreshes.
                    if (changed) requireActivity().recreate();
                }));

        updateNotificationBadge();
        loadProfileAvatar(session);
    }

    private void openPolicy(String baseUrl, String slug, String title) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("url", baseUrl + "/chinh-sach/" + slug);
        androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
    }

    private void loadProfileAvatar(SessionManager session) {
        if (binding == null) return;

        applyDefaultAvatarState();

        String profileId = resolveProfileId(session);
        if (profileId == null || profileId.isEmpty()) return;

        apiService.getProfileById(profileId).enqueue(new Callback<ProfileDto>() {
            @Override
            public void onResponse(@NonNull Call<ProfileDto> call, @NonNull Response<ProfileDto> response) {
                if (!isAdded() || binding == null) return;
                
                ProfileDto profile = response.isSuccessful() ? response.body() : null;
                if (profile != null) {
                    session.saveProfileId(profile.getId());
                    
                    String avatarUrl = profile.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        binding.ivUserAvatar.setPadding(0, 0, 0, 0);
                        binding.ivUserAvatar.setImageTintList(null);
                        binding.ivUserAvatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_account)
                                .error(R.drawable.ic_account)
                                .circleCrop()
                                .into(binding.ivUserAvatar);
                    } else {
                        applyDefaultAvatarState();
                    }

                    if (profile.getName() != null && !profile.getName().isEmpty()) {
                        binding.tvUserName.setText(profile.getName());
                    }
                    if (profile.getPhone() != null && !profile.getPhone().isEmpty()) {
                        binding.tvUserPhone.setText(profile.getPhone());
                    }
                    if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                        binding.tvUserEmail.setText(profile.getEmail());
                        binding.tvUserEmail.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileDto> call, @NonNull Throwable t) {
                // keep session display values if profile fetch fails
            }
        });
    }

    private void applyDefaultAvatarState() {
        if (binding == null || !isAdded()) return;

        int padding = dpToPx(16);
        binding.ivUserAvatar.setImageResource(R.drawable.ic_account);
        binding.ivUserAvatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        binding.ivUserAvatar.setPadding(padding, padding, padding, padding);
        binding.ivUserAvatar.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
        binding.ivUserAvatar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private String resolveProfileId(SessionManager session) {
        String profileId = session.getProfileId();
        if (profileId != null && !profileId.isEmpty()) {
            return profileId;
        }

        String token = session.getToken();
        if (token != null && !token.isEmpty()) {
            session.saveProfileId(token);
            return token;
        }

        var customer = session.getCustomer();
        return customer != null ? customer.getId() : null;
    }

    private void updateNotificationBadge() {
        if (binding == null) return;
        boolean hasUnread = com.unifurniture.mobile.util.NotificationManager.getInstance(requireContext()).hasUnreadNotifications();
        binding.viewNotificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
