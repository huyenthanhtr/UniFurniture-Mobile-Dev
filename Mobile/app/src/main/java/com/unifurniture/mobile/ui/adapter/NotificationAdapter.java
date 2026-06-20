package com.unifurniture.mobile.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.NotificationDto;
import com.unifurniture.mobile.databinding.ItemNotificationBinding;

public class NotificationAdapter extends ListAdapter<NotificationDto, NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationDto notification);
    }

    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        super(new DiffUtil.ItemCallback<NotificationDto>() {
            @Override
            public boolean areItemsTheSame(@NonNull NotificationDto oldItem, @NonNull NotificationDto newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull NotificationDto oldItem, @NonNull NotificationDto newItem) {
                return oldItem.isRead == newItem.isRead &&
                        oldItem.title.equals(newItem.title) &&
                        oldItem.content.equals(newItem.content) &&
                        oldItem.timestamp == newItem.timestamp;
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;
        private final Context context;

        public ViewHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        public void bind(NotificationDto item, OnNotificationClickListener listener) {
            binding.tvTitle.setText(item.title);
            binding.tvContent.setText(item.content);

            // Relative Time formatting
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            binding.tvTime.setText(relativeTime);

            // Visual properties based on read/unread
            if (item.isRead) {
                binding.cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
                binding.viewUnreadDot.setVisibility(View.GONE);
            } else {
                binding.cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.notification_unread));
                binding.viewUnreadDot.setVisibility(View.VISIBLE);
            }

            // Theme icon type
            if ("order".equals(item.type)) {
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_cart);
                binding.ivNotificationIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
                // Soft background tint for order icon container
                binding.cardIconContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.notification_unread));
            } else {
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_account);
                binding.ivNotificationIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent)));
                // Soft gold/amber tint background
                binding.cardIconContainer.setCardBackgroundColor(ColorStateList.valueOf(0xFFFFF9E6));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }
    }
}
