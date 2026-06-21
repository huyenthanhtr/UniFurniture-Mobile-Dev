package com.unifurniture.mobile.ui.account;

import android.content.Intent;
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
import com.unifurniture.mobile.util.CustomBlueDialog;
import com.bumptech.glide.Glide;

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
                    CustomBlueDialog.show(
                            requireContext(),
                            R.drawable.ic_account,
                            getString(R.string.login_required_title),
                            getString(R.string.login_required_msg),
                            getString(R.string.login),
                            dialog -> {
                                dialog.dismiss();
                                startActivity(new Intent(requireContext(), AuthActivity.class));
                            },
                            getString(R.string.cancel),
                            dialog -> dialog.dismiss()
                    ));
            binding.itemMyReviews.setOnClickListener(v -> 
                    CustomBlueDialog.show(
                            requireContext(),
                            R.drawable.ic_account,
                            getString(R.string.login_required_title),
                            getString(R.string.login_required_msg),
                            getString(R.string.login),
                            dialog -> {
                                dialog.dismiss();
                                startActivity(new Intent(requireContext(), AuthActivity.class));
                            },
                            getString(R.string.cancel),
                            dialog -> dialog.dismiss()
                    ));
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

        binding.itemPolicy.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("title", getString(R.string.account_policy_title));
            bundle.putString("url", baseUrl + "/chinh-sach-bao-mat");
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
        });

        binding.itemCommunity.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("title", getString(R.string.account_community_title));
            bundle.putString("url", baseUrl + "/cong-dong");
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
        });

        binding.itemLanguage.setOnClickListener(v ->
                com.unifurniture.mobile.util.LanguageDialog.show(requireContext(), false, (code, changed) -> {
                    // Sec 2: apply immediately by recreating the activity so the whole UI refreshes.
                    if (changed) requireActivity().recreate();
                }));

        updateNotificationBadge();
        loadProfileAvatar(session);
    }

    private void loadProfileAvatar(SessionManager session) {
        var customer = session.getCustomer();
        if (customer == null || customer.getId() == null || customer.getId().isEmpty()) return;

        String cachedAvatar = session.getAvatarUrl();
        if (cachedAvatar != null && !cachedAvatar.isEmpty() && binding != null) {
            Glide.with(requireContext())
                    .load(cachedAvatar)
                    .placeholder(R.drawable.ic_account)
                    .error(R.drawable.ic_account)
                    .into(binding.ivUserAvatar);
            return;
        }

        apiService.getProfile(customer.getId()).enqueue(new Callback<ApiListResponse<ProfileDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiListResponse<ProfileDto>> call, @NonNull Response<ApiListResponse<ProfileDto>> response) {
                if (!isAdded() || binding == null) return;
                ProfileDto profile = response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()
                        ? response.body().getData().get(0) : null;
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    session.saveAvatarUrl(avatarUrl);
                    Glide.with(requireContext())
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_account)
                            .error(R.drawable.ic_account)
                            .into(binding.ivUserAvatar);
                }
                if (profile != null) {
                    session.saveProfileId(profile.getId());
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
            public void onFailure(@NonNull Call<ApiListResponse<ProfileDto>> call, @NonNull Throwable t) {
                // keep session display values if profile fetch fails
            }
        });
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
