package com.unifurniture.mobile.ui.product;

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
import com.unifurniture.mobile.databinding.FragmentWishlistBinding;
import com.unifurniture.mobile.ui.adapter.WishlistAdapter;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private WishlistViewModel viewModel;
    private WishlistAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(WishlistViewModel.class);
        
        setupRecyclerView();
        observeData();
        
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        
        viewModel.loadWishlist();
    }

    private void setupRecyclerView() {
        adapter = new WishlistAdapter(new WishlistAdapter.OnWishlistClickListener() {
            @Override
            public void onClick(com.unifurniture.mobile.data.model.WishlistItemDto item) {
                com.unifurniture.mobile.data.model.ProductDto product = item.getProduct();
                if (product != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("slug", product.slug != null ? product.slug : product.id);
                    Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, bundle);
                }
            }

            @Override
            public void onRemove(com.unifurniture.mobile.data.model.WishlistItemDto item) {
                viewModel.removeFromWishlist(item.id);
            }
        });
        
        binding.rvWishlist.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWishlist.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getWishlist().observe(getViewLifecycleOwner(), list -> {
            boolean isEmpty = list == null || list.isEmpty();
            binding.layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.rvWishlist.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            adapter.submitList(list);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> 
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
