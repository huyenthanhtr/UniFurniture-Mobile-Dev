package com.unifurniture.mobile.ui.checkout;

import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.CheckoutResponse;
import com.unifurniture.mobile.data.model.CartItemDto;
import com.unifurniture.mobile.data.model.CustomerAddressDto;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.data.model.ProductVariantDto;
import com.unifurniture.mobile.data.model.VoucherDto;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.CartManager;
import com.unifurniture.mobile.util.VoucherManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private CheckoutViewModel viewModel;
    private List<CartItemDto> cartItems;
    private String couponCode;
    private double subtotal = 0;
    private double total = 0;
    private double discount = 0;
    private double deposit = 0;
    private boolean requireDeposit = false;
    private String phone = "";
    private List<CustomerAddressDto> savedAddresses = new ArrayList<>();

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

        setupVoucherSection();
        setupAddressSection(savedInstanceState);
        calculatePrices();
        setupLabels();
        detectZone();

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());

        setupInputListeners();

        viewModel.getResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.orderId != null) {
                handleOrderSuccess(response);
            } else {
                String errorMsg = (response != null && response.message != null) ? response.message : getString(R.string.order_failed);
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnPlaceOrder.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
        
        viewModel.getAddresses().observe(getViewLifecycleOwner(), addresses -> {
            if (addresses != null && !addresses.isEmpty()) {
                this.savedAddresses = addresses;
                binding.cardSavedAddress.setVisibility(View.VISIBLE);
                setupAddressSpinner();
            } else {
                binding.cardSavedAddress.setVisibility(View.GONE);
            }
        });
    }

    private void setupLabels() {
        binding.tvLabelName.setText(Html.fromHtml(getString(R.string.label_shipping_name)));
        binding.tvLabelPhone.setText(Html.fromHtml(getString(R.string.label_shipping_phone)));
        binding.tvLabelAddress.setText(Html.fromHtml(getString(R.string.label_shipping_address)));
        binding.tvLabelProvince.setText(Html.fromHtml(getString(R.string.label_shipping_province)));
        binding.tvLabelDistrict.setText(Html.fromHtml(getString(R.string.label_shipping_district)));
    }

    private void setupVoucherSection() {
        binding.btnSelectVoucher.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("subtotal", subtotal);
            bundle.putString("entry_mode", "apply");
            Navigation.findNavController(requireView()).navigate(R.id.voucherListFragment, bundle);
        });

        VoucherDto selectedVoucher = VoucherManager.getInstance(requireContext()).getSelectedVoucher();
        if (selectedVoucher != null) {
            couponCode = selectedVoucher.code;
            binding.tvVoucherLabel.setText(getString(R.string.applied_voucher, couponCode));
        } else {
            couponCode = null;
        }
    }

    private void setupAddressSection(Bundle savedInstanceState) {
        SessionManager session = SessionManager.getInstance(requireContext());
        if (session.isLoggedIn()) {
            CustomerDto customer = session.getCustomer();
            if (savedInstanceState == null && customer != null) {
                if (customer.name != null) binding.etName.setText(customer.name);
                if (customer.phone != null) binding.etPhone.setText(customer.phone);
            }
            viewModel.loadSavedAddresses(session.getCustomerId());
        } else {
            binding.cardSavedAddress.setVisibility(View.GONE);
        }

        binding.btnAddNewAddress.setOnClickListener(v -> {
            binding.etName.setText("");
            binding.etPhone.setText("");
            binding.etAddress.setText("");
            binding.etProvince.setText("");
            binding.etDistrict.setText("");
            binding.spinnerSavedAddress.setSelection(0);
        });
    }

    private void setupAddressSpinner() {
        List<String> displayList = new ArrayList<>();
        displayList.add(getString(R.string.select_address_hint));
        for (CustomerAddressDto addr : savedAddresses) {
            displayList.add(addr.toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, displayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSavedAddress.setAdapter(adapter);

        binding.spinnerSavedAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    CustomerAddressDto selected = savedAddresses.get(position - 1);
                    binding.etName.setText(selected.fullName);
                    binding.etPhone.setText(selected.phone);
                    binding.etAddress.setText(selected.address);
                    binding.etProvince.setText(selected.province);
                    binding.etDistrict.setText(selected.district);
                    detectZone();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupInputListeners() {
        TextWatcher addressWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                detectZone();
            }
        };

        binding.etProvince.addTextChangedListener(addressWatcher);
        binding.etDistrict.addTextChangedListener(addressWatcher);
    }

    private void calculatePrices() {
        subtotal = 0;
        if (cartItems != null) {
            for (CartItemDto item : cartItems) {
                subtotal += item.getUnitPrice() * (item.quantity != null ? item.quantity : 1);
            }
        }

        discount = 0;
        VoucherDto voucher = VoucherManager.getInstance(requireContext()).getSelectedVoucher();
        if (voucher != null) {
            couponCode = voucher.code;
            discount = VoucherManager.getInstance(requireContext()).calculateDiscount(voucher, subtotal);
            binding.tvVoucherLabel.setText(getString(R.string.applied_voucher, couponCode));
        } else {
            couponCode = null;
        }

        total = Math.max(0, subtotal - discount);
        requireDeposit = total >= 10000000;
        deposit = requireDeposit ? Math.round(total * 0.1) : 0;
        double remaining = total - deposit;

        binding.tvSubtotal.setText(FormatUtil.formatCurrency(subtotal));
        binding.tvDiscount.setText("-" + FormatUtil.formatCurrency(discount));
        binding.tvTotal.setText(FormatUtil.formatCurrency(total));

        if (requireDeposit) {
            binding.layoutDepositAlert.setVisibility(View.VISIBLE);
            binding.layoutDepositSummary.setVisibility(View.VISIBLE);
            binding.tvDepositSummary.setText(FormatUtil.formatCurrency(deposit));
            binding.tvRemainingSummary.setText(FormatUtil.formatCurrency(remaining));
            
            binding.rbBank.setChecked(true);
            binding.rbCod.setEnabled(false);
        } else {
            binding.layoutDepositAlert.setVisibility(View.GONE);
            binding.layoutDepositSummary.setVisibility(View.GONE);
            binding.rbCod.setEnabled(true);
        }
    }

    private void detectZone() {
        String pStr = binding.etProvince.getText().toString();
        String dStr = binding.etDistrict.getText().toString();
        
        String province = normalize(pStr);
        String district = normalize(dStr);

        if (province.isEmpty() && district.isEmpty()) {
            binding.rbGiaoVaLap.setEnabled(true);
            binding.layoutShippingWarning.setVisibility(View.GONE);
            return;
        }

        boolean canInstall = false;
        String combined = (province + " " + district).trim();

        // Logic based on UniFurniture Web, using a combined check for robustness
        if (combined.contains("ho chi minh") || combined.contains("tp hcm") || combined.contains("tphcm")) {
            if (!combined.contains("can gio")) {
                canInstall = true;
            }
        } else if (combined.contains("dong nai")) {
            if (combined.contains("bien hoa")) {
                canInstall = true;
            }
        } else if (combined.contains("binh duong")) {
            if (combined.contains("di an") || combined.contains("thuan an") || 
                combined.contains("thu dau mot") || combined.contains("tan uyen")) {
                canInstall = true;
            }
        }

        if (canInstall) {
            binding.rbGiaoVaLap.setEnabled(true);
            binding.layoutShippingWarning.setVisibility(View.GONE);
        } else {
            binding.rbGiaoKhongLap.setChecked(true);
            binding.rbGiaoVaLap.setEnabled(false);
            
            // Only show warning if user has entered some info
            if (!province.isEmpty() || !district.isEmpty()) {
                binding.layoutShippingWarning.setVisibility(View.VISIBLE);
            } else {
                binding.layoutShippingWarning.setVisibility(View.GONE);
            }
        }
    }

    private String normalize(String str) {
        if (str == null) return "";
        try {
            String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            String out = pattern.matcher(temp).replaceAll("");
            return out.toLowerCase()
                    .replace("đ", "d")
                    .replace("Đ", "d")
                    .trim();
        } catch (Exception e) {
            return str.toLowerCase().trim();
        }
    }

    private void placeOrder() {
        String name = binding.etName.getText().toString().trim();
        phone = binding.etPhone.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String province = binding.etProvince.getText().toString().trim();
        String district = binding.etDistrict.getText().toString().trim();
        String shippingMethod = binding.rgShippingMethod.getCheckedRadioButtonId() == R.id.rbGiaoVaLap ? "GIAO_VA_LAP" : "GIAO_KHONG_LAP";
        String paymentMethod = binding.rgPayment.getCheckedRadioButtonId() == R.id.rbBank ? "CHUYEN_KHOAN" : "COD";

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || province.isEmpty() || district.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.fill_required), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.startsWith("0") || phone.length() < 10 || phone.length() > 11) {
            Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }

        List<CheckoutRequest.Item> items = new ArrayList<>();
        if (cartItems != null) {
            for (CartItemDto cartItem : cartItems) {
                ProductDto p = cartItem.getProduct();
                ProductVariantDto variant = cartItem.getVariant();
                items.add(new CheckoutRequest.Item(
                        cartItem.productId,
                        cartItem.getVariantId(),
                        cartItem.quantity != null ? cartItem.quantity : 1,
                        cartItem.getUnitPrice(),
                        p != null ? p.name : getString(R.string.product_default_name),
                        variant != null ? (variant.color != null ? variant.color : getString(R.string.variant_default_name)) : getString(R.string.variant_default_name),
                        variant != null ? variant.sku : "-"
                ));
            }
        }

        SessionManager session = SessionManager.getInstance(requireContext());
        String accountId = session.getProfileId();
        if (accountId == null || accountId.trim().isEmpty()) {
            accountId = session.getCustomerId();
        }
        CheckoutRequest req = new CheckoutRequest(accountId, session.getCartId(), name, phone, address, paymentMethod);
        req.shippingEmail = email.isEmpty() ? null : email;
        req.province = province;
        req.district = district;
        req.shippingMethod = shippingMethod;
        req.couponCode = couponCode;
        req.couponDiscount = discount;
        req.totalAmount = total;
        req.depositAmount = deposit;
        req.items = items;

        viewModel.placeOrder(req);
    }

    private void handleOrderSuccess(CheckoutResponse response) {
        if (couponCode != null) {
            VoucherManager.getInstance(requireContext()).markAsUsed(couponCode);
            VoucherManager.getInstance(requireContext()).setSelectedVoucherCode(null);
        }
        viewModel.clearCartItems(cartItems);
        String orderId = response.orderId;
        String orderCode = response.orderCode != null ? response.orderCode : "UF-XXXXXX";

        // The backend persists this "order placed" notification (createCheckoutOrder -> sendToCustomer)
        // and the app syncs it from /api/notifications; adding a local copy here would duplicate it.

        Bundle bundle = new Bundle();
        bundle.putString("order_id", orderId);
        bundle.putString("order_code", orderCode);
        String paymentMethod = binding.rgPayment.getCheckedRadioButtonId() == R.id.rbBank ? "CHUYEN_KHOAN" : "COD";
        if ("CHUYEN_KHOAN".equals(paymentMethod)) {
            bundle.putFloat("total_amount", (float) total);
            bundle.putFloat("deposit_amount", (float) deposit);
            bundle.putBoolean("require_deposit", requireDeposit);
            bundle.putString("shipping_phone", phone);
            Navigation.findNavController(requireView()).navigate(R.id.paymentQrFragment, bundle);
        } else {
            Navigation.findNavController(requireView()).navigate(R.id.orderSuccessFragment, bundle);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class CheckoutViewModel extends AndroidViewModel {
        private final OrderRepository repository;
        private final MutableLiveData<CheckoutResponse> result = new MutableLiveData<>();
        private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
        private final MutableLiveData<List<CustomerAddressDto>> addresses = new MutableLiveData<>();

        public CheckoutViewModel(@NonNull Application application) {
            super(application);
            repository = new OrderRepository(UniFurnitureApp.getInstance().getApiService());
        }

        public void loadSavedAddresses(String customerId) {
            if (customerId == null) return;
            UniFurnitureApp.getInstance().getApiService().getCustomerAddresses(customerId, 50)
                .enqueue(new retrofit2.Callback<ApiListResponse<CustomerAddressDto>>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<ApiListResponse<CustomerAddressDto>> call,
                                           @NonNull retrofit2.Response<ApiListResponse<CustomerAddressDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            addresses.setValue(response.body().items);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull retrofit2.Call<ApiListResponse<CustomerAddressDto>> call, @NonNull Throwable t) {}
                });
        }

        public void placeOrder(CheckoutRequest req) {
            loading.setValue(true);
            repository.createOrder(req).observeForever(r -> {
                result.setValue(r);
                loading.setValue(false);
            });
        }

        public void clearCartItems(List<CartItemDto> items) {
            if (items == null) return;
            pruneCheckedOutItems(items);
            for (CartItemDto item : items) {
                if (item.id != null) {
                    UniFurnitureApp.getInstance().getApiService().deleteCartItem(item.id).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                        @Override public void onResponse(@NonNull retrofit2.Call<com.google.gson.JsonObject> call, @NonNull retrofit2.Response<com.google.gson.JsonObject> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                com.google.gson.JsonObject jsonObj = response.body();
                                if (!jsonObj.has("deleted")) {
                                    com.unifurniture.mobile.data.model.CartDto cart = new com.google.gson.Gson().fromJson(jsonObj, com.unifurniture.mobile.data.model.CartDto.class);
                                    CartManager.getInstance().updateCart(cart);
                                }
                            }
                        }
                        @Override public void onFailure(@NonNull retrofit2.Call<com.google.gson.JsonObject> call, @NonNull Throwable t) {}
                    });
                }
            }
        }

        private void pruneCheckedOutItems(List<CartItemDto> checkedOutItems) {
            com.unifurniture.mobile.data.model.CartDto current = CartManager.getInstance().getCurrentCart();
            if (current == null || current.items == null) return;

            java.util.Set<String> checkedOutIds = new java.util.HashSet<>();
            for (CartItemDto item : checkedOutItems) {
                if (item != null && item.id != null) {
                    checkedOutIds.add(item.id);
                }
            }
            if (checkedOutIds.isEmpty()) return;

            com.unifurniture.mobile.data.model.CartDto updated = new com.unifurniture.mobile.data.model.CartDto();
            updated.id = current.id;
            updated.customerId = current.customerId;
            updated.items = new java.util.ArrayList<>();
            double total = 0;
            for (CartItemDto item : current.items) {
                if (item != null && !checkedOutIds.contains(item.id)) {
                    updated.items.add(item);
                    total += item.getTotalPrice();
                }
            }
            updated.total = total;
            CartManager.getInstance().updateCart(updated);
        }

        public LiveData<CheckoutResponse> getResult() { return result; }
        public LiveData<Boolean> isLoading() { return loading; }
        public LiveData<List<CustomerAddressDto>> getAddresses() { return addresses; }
    }
}
