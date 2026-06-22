package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentMyReviewsBinding;
import com.unifurniture.mobile.ui.adapter.MyReviewsAdapter;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReviewsFragment extends Fragment {

    private FragmentMyReviewsBinding binding;
    private MyReviewsAdapter adapter;
    private ApiService apiService;
    private String customerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        customerId = SessionManager.getInstance(requireContext()).getCustomerId();

        setupToolbar();
        setupRecyclerView();

        if (customerId != null) {
            loadReviews();
        } else {
            binding.tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new MyReviewsAdapter();
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(adapter);
    }

    private void loadReviews() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        apiService.getReviews(customerId).enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(Call<List<ReviewDto>> call, Response<List<ReviewDto>> response) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    List<ReviewDto> reviews = response.body();
                    if (response.isSuccessful() && reviews != null) {
                        if (!reviews.isEmpty()) {
                            adapter.submitList(reviews);
                        } else {
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                        }
                    } else {
                        ToastUtil.error(requireContext(), R.string.error_unknown);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ReviewDto>> call, Throwable t) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
                    binding.tvEmpty.setVisibility(View.VISIBLE);
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
