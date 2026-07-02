package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.databinding.FragmentMyReviewsBinding;
import com.unifurniture.mobile.ui.adapter.MyReviewsAdapter;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;

public class MyReviewsFragment extends Fragment {

    private FragmentMyReviewsBinding binding;
    private MyReviewsAdapter adapter;
    private MyReviewsViewModel viewModel;
    private String customerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MyReviewsViewModel.class);
        customerId = SessionManager.getInstance(requireContext()).getCustomerId();

        setupToolbar();
        setupRecyclerView();
        observeViewModel();

        if (customerId != null) {
            viewModel.loadReviewsIfNeeded();
        } else {
            binding.tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && SessionManager.getInstance(requireContext()).getCustomerId() != null) {
            viewModel.refresh();
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        String serverHost = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");
        adapter = new MyReviewsAdapter(serverHost, review -> {
            String slug = review.getProductSlug();
            if (slug == null || slug.trim().isEmpty() || !isAdded()) {
                return;
            }
            Bundle args = new Bundle();
            args.putString("slug", slug.trim());
            Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
        });
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getReviews().observe(getViewLifecycleOwner(), reviews -> {
            if (binding == null) return;
            if (reviews != null && !reviews.isEmpty()) {
                adapter.submitList(reviews);
                binding.rvReviews.setVisibility(View.VISIBLE);
                binding.tvEmpty.setVisibility(View.GONE);
            } else {
                adapter.submitList(java.util.Collections.emptyList());
                binding.rvReviews.setVisibility(View.GONE);
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (binding == null || error == null || error.isEmpty()) return;
            if ("load_failed".equals(error)) {
                ToastUtil.error(requireContext(), R.string.error_unknown);
            } else {
                ToastUtil.error(requireContext(), getString(R.string.error_network, error));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
