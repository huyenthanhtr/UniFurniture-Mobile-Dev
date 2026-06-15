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
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Setup recycler view
        adapter = new NotificationAdapter(notification -> {
            // Mark as read
            NotificationManager.getInstance(requireContext()).markAsRead(notification.id);
            loadNotifications();

            // Perform actions based on type
            if ("order".equals(notification.type) && notification.orderId != null) {
                Toast.makeText(requireContext(), getString(R.string.toast_order_id, notification.orderId), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), notification.title, Toast.LENGTH_SHORT).show();
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

        int activeTextColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.gray_600);

        // Reset tabs style
        binding.tabAll.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabAll.setTextColor(inactiveTextColor);

        binding.tabOrders.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabOrders.setTextColor(inactiveTextColor);

        binding.tabAccount.setBackgroundResource(R.drawable.bg_tab_pill_unselected);
        binding.tabAccount.setTextColor(inactiveTextColor);

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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
