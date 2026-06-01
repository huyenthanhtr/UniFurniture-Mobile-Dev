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
import com.unifurniture.mobile.data.repository.OrderRepository;
import com.unifurniture.mobile.databinding.FragmentCheckoutBinding;
import com.unifurniture.mobile.util.SessionManager;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.CheckoutResponse;

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
                Toast.makeText(requireContext(), getString(R.string.fill_required), Toast.LENGTH_SHORT).show();
                return;
            }

            String customerId = session.getCustomerId();
            String cartId = session.getCartId();
            viewModel.placeOrder(customerId, cartId, name, phone, address, paymentMethod);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.order != null) {
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

        public void placeOrder(String customerId, String cartId, String name,
                               String phone, String address, String paymentMethod) {
            loading.setValue(true);
            CheckoutRequest req = new CheckoutRequest(customerId, cartId, name, phone, address, paymentMethod);
            repository.createOrder(req).observeForever(r -> {
                result.setValue(r);
                loading.setValue(false);
            });
        }

        public LiveData<CheckoutResponse> getResult() { return result; }
        public LiveData<Boolean> isLoading() { return loading; }
    }
}
