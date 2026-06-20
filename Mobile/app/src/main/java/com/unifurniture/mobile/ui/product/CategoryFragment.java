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

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentCategoryBinding;
import com.unifurniture.mobile.ui.adapter.CategoryAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private CategoryAdapter adapter;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        setupRecyclerView();
        loadCategories();
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdapter(category -> {
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.id);
            bundle.putString("categoryName", category.name);
            Navigation.findNavController(requireView()).navigate(R.id.productListFragment, bundle);
        });
        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvCategories.setAdapter(adapter);
    }

    private void loadCategories() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        apiService.getCategories(1, 100, "active").enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(Call<List<CategoryDto>> call, Response<List<CategoryDto>> response) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        if (!response.body().isEmpty()) {
                            adapter.submitList(response.body());
                        } else {
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<CategoryDto>> call, Throwable t) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.error_network, t.getMessage()), Toast.LENGTH_SHORT).show();
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
