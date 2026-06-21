package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.databinding.FragmentOrderListBinding;
import com.unifurniture.mobile.ui.adapter.OrderAdapter;
import com.unifurniture.mobile.util.NavViewModelProvider;
import com.unifurniture.mobile.util.RecyclerViewStateHelper;
import com.unifurniture.mobile.util.SessionManager;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.repository.OrderRepository;
import com.unifurniture.mobile.util.LiveDataUtil;

public class OrderListFragment extends Fragment {

    private FragmentOrderListBinding binding;
    private OrderListViewModel viewModel;
    private OrderAdapter adapter;
    private final RecyclerViewStateHelper rvState = new RecyclerViewStateHelper("orders");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = NavViewModelProvider.get(this, R.id.orderListFragment, OrderListViewModel.class);
        viewModel.loadOrdersIfNeeded();

        adapter = new OrderAdapter(order -> {
            Bundle bundle = new Bundle();
            bundle.putString("order_id", order.getId());
            Navigation.findNavController(requireView()).navigate(R.id.orderDetailFragment, bundle);
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);
        rvState.bind(binding.rvOrders, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        viewModel.getOrders().observe(getViewLifecycleOwner(), response -> {
            boolean isEmpty = response == null || response.items == null || response.items.isEmpty();
            binding.tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (!isEmpty) adapter.submitList(response.items, rvState.afterSubmitCallback());
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
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

    // ── Inline ViewModel ──────────────────────────────────────────────────────
    public static class OrderListViewModel extends AndroidViewModel {
        private final OrderRepository repository;
        private final MutableLiveData<ApiListResponse<OrderDto>> orders = new MutableLiveData<>();
        private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
        private boolean hasLoaded = false;

        public OrderListViewModel(@NonNull Application application) {
            super(application);
            repository = new OrderRepository(UniFurnitureApp.getInstance().getApiService());
        }

        public void loadOrdersIfNeeded() {
            if (hasLoaded) return;
            hasLoaded = true;
            String customerId = SessionManager.getInstance(getApplication()).getCustomerId();
            if (customerId == null) return;
            loading.setValue(true);
            LiveDataUtil.observeOnce(repository.getOrders(customerId), r -> {
                orders.setValue(r);
                loading.setValue(false);
            });
        }

        public LiveData<ApiListResponse<OrderDto>> getOrders() { return orders; }
        public LiveData<Boolean> isLoading() { return loading; }
    }
}
