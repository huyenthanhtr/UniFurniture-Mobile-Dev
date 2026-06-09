package com.unifurniture.mobile.ui.checkout;

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
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CartDto;
import com.unifurniture.mobile.data.model.CartItemDto;
import com.unifurniture.mobile.data.model.CheckoutItem;
import com.unifurniture.mobile.data.model.CheckoutRequest;
import com.unifurniture.mobile.data.repository.CartRepository;
import com.unifurniture.mobile.data.repository.OrderRepository;
import com.unifurniture.mobile.databinding.FragmentCheckoutBinding;
import com.unifurniture.mobile.util.SessionManager;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CheckoutResponse;

import java.util.ArrayList;
import java.util.List;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private CheckoutViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);

        SessionManager session = SessionManager.getInstance(requireContext());
        if (!session.isLoggedIn()) {
            requireActivity().onBackPressed();
            return;
        }

        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnPlaceOrder.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            String address = binding.etAddress.getText().toString().trim();
            String paymentMethod = binding.rgPayment.getCheckedRadioButtonId() == R.id.rbCod ? "cod" : "bank_transfer";

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(requireContext(), R.string.str_please_fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            String accountId = session.getProfileId();
            viewModel.placeOrder(accountId, name, phone, address, paymentMethod);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.orderId != null) {
                Toast.makeText(requireContext(), R.string.str_order_success, Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
            } else {
                Toast.makeText(requireContext(), R.string.str_order_failed, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnPlaceOrder.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Inline ViewModel ──────────────────────────────────────────────────────
    public static class CheckoutViewModel extends AndroidViewModel {
        private final OrderRepository orderRepository;
        private final CartRepository cartRepository;
        private final MutableLiveData<CheckoutResponse> result = new MutableLiveData<>();
        private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

        public CheckoutViewModel(@NonNull Application application) {
            super(application);
            orderRepository = new OrderRepository(UniFurnitureApp.getInstance().getApiService());
            cartRepository = new CartRepository(UniFurnitureApp.getInstance().getApiService());
        }

        /**
         * Place order:
         * 1. Load active cart to get items
         * 2. Build CheckoutRequest with items[] array
         * 3. POST to /orders
         */
        public void placeOrder(String accountId, String name,
                               String phone, String address, String paymentMethod) {
            loading.setValue(true);
            // Step 1: Load cart
            cartRepository.getActiveCart(accountId).observeForever(cart -> {
                if (cart == null || cart.items == null || cart.items.isEmpty()) {
                    loading.setValue(false);
                    result.setValue(null);
                    return;
                }

                // Step 2: Build items array from cart items
                List<CheckoutItem> checkoutItems = new ArrayList<>();
                double totalAmount = 0;
                for (CartItemDto cartItem : cart.items) {
                    CheckoutItem ci = new CheckoutItem();
                    ci.variantId = cartItem.getVariantId();
                    ci.productId = cartItem.getProductId();
                    ci.productName = cartItem.getProductName();
                    ci.variantName = (cartItem.variant != null && cartItem.variant.variantName != null)
                            ? cartItem.variant.variantName : "";
                    ci.sku = (cartItem.variant != null && cartItem.variant.sku != null)
                            ? cartItem.variant.sku : "";
                    ci.quantity = cartItem.quantity != null ? cartItem.quantity : 1;
                    ci.unitPrice = cartItem.getEffectivePrice();
                    checkoutItems.add(ci);
                    totalAmount += ci.unitPrice * ci.quantity;
                }

                // Step 3: Create checkout request
                CheckoutRequest req = new CheckoutRequest(
                        accountId, name, phone, address, paymentMethod, checkoutItems
                );
                req.totalAmount = totalAmount;

                // Step 4: Submit order
                orderRepository.createOrder(req).observeForever(r -> {
                    result.setValue(r);
                    loading.setValue(false);
                });
            });
        }

        public LiveData<CheckoutResponse> getResult() { return result; }
        public LiveData<Boolean> isLoading() { return loading; }
    }
}
