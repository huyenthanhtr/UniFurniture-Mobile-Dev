package com.unifurniture.mobile.ui.account;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.NotificationDto;
import com.unifurniture.mobile.databinding.FragmentNotificationBinding;
import com.unifurniture.mobile.ui.adapter.NotificationAdapter;
import com.unifurniture.mobile.util.NotificationManager;
import com.unifurniture.mobile.util.ToastUtil;
import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private NotificationAdapter adapter;
    private String currentFilter = "all"; // "all", "order", "account"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Back press
        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Setup recycler view
        adapter = new NotificationAdapter(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationDto notification) {
                NotificationManager.getInstance(requireContext()).markAsRead(notification.id);
                loadNotifications();

                if ("order".equals(notification.type) && notification.orderId != null) {
                    ToastUtil.show(requireContext(), getString(R.string.toast_order_id, notification.orderId));
                } else {
                    ToastUtil.show(requireContext(), notification.title);
                }
            }

            @Override
            public void onMarkUnreadClick(NotificationDto notification) {
                NotificationManager.getInstance(requireContext()).markAsUnread(notification.id);
                loadNotifications();
            }
        });

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        // Setup Mark All As Read
        binding.btnMarkAllRead.setOnClickListener(v -> {
            NotificationManager.getInstance(requireContext()).markAllAsRead();
            loadNotifications();
            ToastUtil.show(requireContext(), R.string.toast_mark_all_read);
        });

        // Setup Tab filters
        binding.tabAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateTabStyles();
            loadNotifications();
        });

        binding.tabOrders.setOnClickListener(v -> {
            currentFilter = "order";
            updateTabStyles();
            loadNotifications();
        });

        binding.tabAccount.setOnClickListener(v -> {
            currentFilter = "account";
            updateTabStyles();
            loadNotifications();
        });

        binding.tabOther.setOnClickListener(v -> {
            currentFilter = "other";
            updateTabStyles();
            loadNotifications();
        });

        // Initial loading
        loadNotifications();
        updateTabStyles();
    }

    private void loadNotifications() {
        if (binding == null) return;

        List<NotificationDto> rawList = NotificationManager.getInstance(requireContext()).getNotifications();
        List<NotificationDto> filteredList = new ArrayList<>();

        for (NotificationDto item : rawList) {
            if ("all".equals(currentFilter)) {
                filteredList.add(item);
            } else if (currentFilter.equals(item.type)) {
                filteredList.add(item);
            }
        }

        adapter.submitList(filteredList);

        if (filteredList.isEmpty()) {
            binding.rvNotifications.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvNotifications.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void updateTabStyles() {
        if (binding == null) return;

        styleTab(binding.tabAll, R.color.primary, "all".equals(currentFilter));
        styleTab(binding.tabOrders, R.color.notif_order, "order".equals(currentFilter));
        styleTab(binding.tabAccount, R.color.notif_account, "account".equals(currentFilter));
        styleTab(binding.tabOther, R.color.notif_other, "other".equals(currentFilter));
    }

    private void styleTab(TextView tab, int colorRes, boolean active) {
        int typeColor = ContextCompat.getColor(requireContext(), colorRes);
        tab.setBackgroundResource(active ? R.drawable.bg_tab_pill_selected : R.drawable.bg_tab_pill_unselected);
        tab.setBackgroundTintList(ColorStateList.valueOf(active
                ? typeColor
                : ContextCompat.getColor(requireContext(), R.color.white)));
        tab.setTextColor(active
                ? ContextCompat.getColor(requireContext(), R.color.white)
                : typeColor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
