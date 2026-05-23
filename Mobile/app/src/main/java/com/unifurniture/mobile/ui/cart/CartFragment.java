package com.unifurniture.mobile.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentCartBinding;
import com.unifurniture.mobile.ui.adapter.CartItemAdapter;
import com.unifurniture.mobile.ui.auth.AuthActivity;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.SessionManager;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartItemAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Check login
        if (!SessionManager.getInstance(requireContext()).isLoggedIn()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.layoutCart.setVisibility(View.GONE);
            binding.btnLoginPrompt.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AuthActivity.class)));
            return;
        }

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        setupRecyclerView();
        observeData();

        binding.btnCheckout.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.checkoutFragment));
    }

    private void setupRecyclerView() {
        adapter = new CartItemAdapter(
                (item, quantity) -> viewModel.updateQuantity(item.id, quantity),
                item -> viewModel.removeItem(item.id)
        );
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            boolean isEmpty = cart == null || cart.items == null || cart.items.isEmpty();
            binding.layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.layoutCart.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (!isEmpty) {
                adapter.submitList(cart.items);
                double total = viewModel.getTotal();
                binding.tvTotal.setText(FormatUtil.formatCurrency(total));
            }
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
