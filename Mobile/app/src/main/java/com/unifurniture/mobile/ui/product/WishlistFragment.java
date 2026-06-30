package com.unifurniture.mobile.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentWishlistBinding;
import com.unifurniture.mobile.ui.adapter.WishlistAdapter;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private WishlistViewModel viewModel;
    private WishlistAdapter adapter;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("wishlist");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.wishlistFragment, WishlistViewModel.class);

        setupRecyclerView(savedInstanceState);
        observeData();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        viewModel.loadWishlistIfNeeded();
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        String serverHost = com.unifurniture.mobile.BuildConfig.API_BASE_URL.replace("/api/", "");
        adapter = new WishlistAdapter(serverHost, new WishlistAdapter.OnWishlistClickListener() {
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
                viewModel.removeFromWishlist(item);
            }
        });

        binding.rvWishlist.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWishlist.setAdapter(adapter);
        rvState.bind(binding.rvWishlist, savedInstanceState);
    }

    private void observeData() {
        viewModel.getWishlist().observe(getViewLifecycleOwner(), list -> {
            boolean isEmpty = list == null || list.isEmpty();
            binding.layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.rvWishlist.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (!isEmpty) {
                adapter.submitList(list, rvState.afterSubmitCallback());
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean initialLoad = Boolean.TRUE.equals(loading)
                    && (viewModel.getWishlist().getValue() == null || viewModel.getWishlist().getValue().isEmpty());
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
