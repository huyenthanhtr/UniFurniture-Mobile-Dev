package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentOrderTrackingBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderTrackingFragment extends Fragment {

    private FragmentOrderTrackingBinding binding;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderTrackingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.btnTrack.setOnClickListener(v -> {
            String trackingCode = binding.etTrackingCode.getText() != null ? binding.etTrackingCode.getText().toString().trim() : "";
            if (trackingCode.isEmpty()) {
                binding.etTrackingCode.setError(getString(R.string.tracking_code_required));
                return;
            }
            trackOrder(trackingCode);
        });
    }

    private void trackOrder(String trackingCode) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnTrack.setEnabled(false);

        apiService.trackOrderByCode(trackingCode).enqueue(new Callback<ApiListResponse<OrderDto>>() {
            @Override
            public void onResponse(Call<ApiListResponse<OrderDto>> call, Response<ApiListResponse<OrderDto>> response) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnTrack.setEnabled(true);
                    
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        if (!response.body().getData().isEmpty()) {
                            OrderDto order = response.body().getData().get(0);
                            // Navigate to OrderDetailFragment
                            Bundle bundle = new Bundle();
                            bundle.putString("order_id", order.getId());
                            Navigation.findNavController(requireView()).navigate(R.id.orderDetailFragment, bundle);
                        } else {
                            Toast.makeText(requireContext(), R.string.tracking_not_found, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.tracking_lookup_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiListResponse<OrderDto>> call, Throwable t) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnTrack.setEnabled(true);
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
