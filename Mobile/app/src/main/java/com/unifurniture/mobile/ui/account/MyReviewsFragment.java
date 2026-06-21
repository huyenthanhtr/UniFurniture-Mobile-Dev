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
import com.unifurniture.mobile.databinding.FragmentMyReviewsBinding;
import com.unifurniture.mobile.ui.adapter.MyReviewsAdapter;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;
import com.unifurniture.mobile.util.SessionManager;

import java.util.List;

public class MyReviewsFragment extends Fragment {

    private FragmentMyReviewsBinding binding;
    private MyReviewsViewModel viewModel;
    private MyReviewsAdapter adapter;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("my_reviews");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.myReviewsFragment, MyReviewsViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        setupRecyclerView(savedInstanceState);
        observeData();

        String customerId = SessionManager.getInstance(requireContext()).getCustomerId();
        if (customerId != null) {
            viewModel.loadReviewsIfNeeded();
        } else {
            binding.tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        adapter = new MyReviewsAdapter();
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(adapter);
        rvState.bind(binding.rvReviews, savedInstanceState);
    }

    private void observeData() {
        viewModel.getReviews().observe(getViewLifecycleOwner(), reviews -> {
            if (reviews == null) {
                if (!Boolean.TRUE.equals(viewModel.isLoading().getValue())) {
                    Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
                return;
            }
            if (reviews.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                adapter.submitList(reviews, rvState.afterSubmitCallback());
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            List<ReviewDto> cached = viewModel.getReviews().getValue();
            boolean initialLoad = Boolean.TRUE.equals(loading) && (cached == null || cached.isEmpty());
            binding.progressBar.setVisibility(initialLoad ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        rvState.savePending();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        rvState.save(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
