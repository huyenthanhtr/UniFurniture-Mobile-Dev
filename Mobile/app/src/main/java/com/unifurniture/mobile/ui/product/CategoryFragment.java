package com.unifurniture.mobile.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.databinding.FragmentCategoryBinding;
import com.unifurniture.mobile.ui.adapter.CategoryAdapter;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;

import java.util.List;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("categories");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.categoryFragment, CategoryViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        setupRecyclerView(savedInstanceState);
        observeData();
        viewModel.loadCategoriesIfNeeded();
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        String serverHost = BuildConfig.API_BASE_URL.replace("/api/", "");
        adapter = new CategoryAdapter(serverHost, category -> {
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.id);
            bundle.putString("categoryName", category.name);
            Navigation.findNavController(requireView()).navigate(R.id.productListFragment, bundle);
        });
        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.rvCategories.setAdapter(adapter);
        rvState.bind(binding.rvCategories, savedInstanceState);
    }

    private void observeData() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            if (list == null) return;
            if (list.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                adapter.submitList(list, rvState.afterSubmitCallback());
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean initialLoad = Boolean.TRUE.equals(loading) && !viewModel.hasCategories();
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
