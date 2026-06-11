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
import com.unifurniture.mobile.data.model.CheckoutRequest;
import com.unifurniture.mobile.data.model.CustomerDto;
import com.unifurniture.mobile.data.repository.OrderRepository;
import com.unifurniture.mobile.databinding.FragmentCheckoutBinding;
import com.unifurniture.mobile.util.SessionManager;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CheckoutResponse;
import com.unifurniture.mobile.data.model.CartItemDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.data.model.ProductVariantDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private CheckoutViewModel viewModel;
    private List<CartItemDto> cartItems;
    private String couponCode;

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

        // Retrieve arguments
        if (getArguments() != null) {
            couponCode = getArguments().getString("coupon_code", null);
            String jsonItems = getArguments().getString("cart_items_json", null);
            if (jsonItems != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<CartItemDto>>(){}.getType();
                cartItems = gson.fromJson(jsonItems, type);
            }
        }

        SessionManager session = SessionManager.getInstance(requireContext());
        
        // Auto-fill if logged in
        if (session.isLoggedIn()) {
            CustomerDto customer = session.getCustomer();
            if (customer != null) {
                if (customer.name != null) binding.etName.setText(customer.name);
                if (customer.phone != null) binding.etPhone.setText(customer.phone);
            }
        }

        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnPlaceOrder.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            String address = binding.etAddress.getText().toString().trim();
            String paymentMethod = binding.rgPayment.getCheckedRadioButtonId() == R.id.rbCod ? "cod" : "bank_transfer";

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.fill_required), Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse address for province and district (server requires them)
            String province = "Hồ Chí Minh";
            String district = "Quận 1";
            String[] parts = address.split(",");
            if (parts.length >= 3) {
                province = parts[parts.length - 1].trim();
                district = parts[parts.length - 2].trim();
            } else if (parts.length == 2) {
                province = parts[1].trim();
                district = parts[0].trim();
            }

            // Map CartItemDto to CheckoutRequest.Item
            List<CheckoutRequest.Item> items = new ArrayList<>();
            if (cartItems != null) {
                for (CartItemDto cartItem : cartItems) {
                    ProductDto p = cartItem.getProduct();
                    ProductVariantDto variant = cartItem.getVariant();
                    String productId = cartItem.productId;
                    String variantId = cartItem.getVariantId();
                    
                    items.add(new CheckoutRequest.Item(
                            productId,
                            variantId,
                            cartItem.quantity != null ? cartItem.quantity : 1,
                            cartItem.getUnitPrice(),
                            p != null ? p.name : "Sản phẩm",
                            variant != null ? (variant.color != null ? variant.color : "Mặc định") : "Mặc định",
                            variant != null ? variant.sku : "-"
                    ));
                }
            }

            String customerId = session.getCustomerId();
            String cartId = session.getCartId();

            CheckoutRequest req = new CheckoutRequest(customerId, cartId, name, phone, address, paymentMethod);
            req.couponCode = couponCode;
            req.province = province;
            req.district = district;
            req.items = items;

            viewModel.placeOrder(req);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.orderId != null) {
                // Mark coupon as used locally
                if (couponCode != null) {
                    com.unifurniture.mobile.util.VoucherManager.getInstance(requireContext())
                            .markAsUsed(couponCode);
                }

                // Trigger local notification
                String orderId = response.orderId;
                String orderCode = response.orderCode != null ? response.orderCode : "UF-XXXXXX";
                com.unifurniture.mobile.util.NotificationManager.getInstance(requireContext())
                        .addNotification(
                                "Đặt hàng thành công!",
                                "Đơn hàng " + orderCode + " của bạn đã được nhận và đang xử lý. Cảm ơn bạn đã mua hàng!",
                                "order",
                                orderId
                        );

                Toast.makeText(requireContext(), getString(R.string.order_success), Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
            } else {
                Toast.makeText(requireContext(), getString(R.string.order_failed), Toast.LENGTH_SHORT).show();
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
        private final OrderRepository repository;
        private final MutableLiveData<CheckoutResponse> result = new MutableLiveData<>();
        private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

        public CheckoutViewModel(@NonNull Application application) {
            super(application);
            repository = new OrderRepository(UniFurnitureApp.getInstance().getApiService());
        }

        public void placeOrder(CheckoutRequest req) {
            loading.setValue(true);
            repository.createOrder(req).observeForever(r -> {
                result.setValue(r);
                loading.setValue(false);
            });
        }

        public LiveData<CheckoutResponse> getResult() { return result; }
        public LiveData<Boolean> isLoading() { return loading; }
    }
}
