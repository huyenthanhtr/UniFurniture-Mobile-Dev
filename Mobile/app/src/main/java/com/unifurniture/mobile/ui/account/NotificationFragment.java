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
                // Mark as read
                NotificationManager.getInstance(requireContext()).markAsRead(notification.id);
                loadNotifications();

                // Perform actions based on type
                if ("order".equals(notification.type) && notification.orderId != null) {
                    Toast.makeText(requireContext(), getString(R.string.toast_order_id, notification.orderId), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), notification.title, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onMarkUnreadClick(NotificationDto notification) {
                NotificationManager.getInstance(requireContext()).markAsUnread(notification.id);
                loadNotifications();
                Toast.makeText(requireContext(), R.string.toast_mark_unread, Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        // Setup Mark All As Read
        binding.btnMarkAllRead.setOnClickListener(v -> {
            NotificationManager.getInstance(requireContext()).markAllAsRead();
            loadNotifications();
            Toast.makeText(requireContext(), R.string.toast_mark_all_read, Toast.LENGTH_SHORT).show();
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
        List<NotificationDto> unreadList = new ArrayList<>();

        for (NotificationDto item : rawList) {
            if (!item.isRead) {
                unreadList.add(item);
            }

            if ("all".equals(currentFilter)) {
                filteredList.add(item);
            } else if (currentFilter.equals(item.type)) {
                filteredList.add(item);
            } else if ("other".equals(currentFilter) && !"order".equals(item.type) && !"account".equals(item.type)) {
                filteredList.add(item);
            }
        }

        updateSummary(unreadList);
        adapter.submitList(filteredList);

        if (filteredList.isEmpty()) {
            binding.rvNotifications.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvNotifications.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void updateSummary(List<NotificationDto> unreadList) {
        if (unreadList.isEmpty()) {
            binding.layoutSummary.setVisibility(View.GONE);
            return;
        }

        binding.layoutSummary.setVisibility(View.VISIBLE);
        
        boolean hasOrder = false;
        boolean hasAccount = false;
        boolean hasOther = false;

        for (NotificationDto n : unreadList) {
            if ("order".equals(n.type)) hasOrder = true;
            else if ("account".equals(n.type)) hasAccount = true;
            else hasOther = true;
        }

        StringBuilder types = new StringBuilder();
        String comma = getString(R.string.comma);
        if (hasOrder) types.append(getString(R.string.order_type));
        if (hasAccount) {
            if (types.length() > 0) types.append(comma);
            types.append(getString(R.string.account_type));
        }
        if (hasOther) {
            if (types.length() > 0) types.append(comma);
            types.append(getString(R.string.other_type));
        }

        binding.tvSummary.setText(getString(R.string.notification_summary, unreadList.size(), types.toString()));
    }

    private void updateTabStyles() {
        if (binding == null) return;

        int activeTextColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.gray_600);

        // Reset tabs style
        binding.tabAll.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabAll.setTextColor(inactiveTextColor);

        binding.tabOrders.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabOrders.setTextColor(inactiveTextColor);

        binding.tabAccount.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabAccount.setTextColor(inactiveTextColor);

        binding.tabOther.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabOther.setTextColor(inactiveTextColor);

        // Highlight active tab
        if ("all".equals(currentFilter)) {
            binding.tabAll.setBackgroundResource(R.drawable.bg_tab_pill_selected);
            binding.tabAll.setTextColor(activeTextColor);
        } else if ("order".equals(currentFilter)) {
            binding.tabOrders.setBackgroundResource(R.drawable.bg_tab_pill_selected);
            binding.tabOrders.setTextColor(activeTextColor);
        } else if ("account".equals(currentFilter)) {
            binding.tabAccount.setBackgroundResource(R.drawable.bg_tab_pill_selected);
            binding.tabAccount.setTextColor(activeTextColor);
        } else if ("other".equals(currentFilter)) {
            binding.tabOther.setBackgroundResource(R.drawable.bg_tab_pill_selected);
            binding.tabOther.setTextColor(activeTextColor);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
