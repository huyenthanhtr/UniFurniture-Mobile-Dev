package com.unifurniture.mobile.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
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
        void onMarkUnreadClick(NotificationDto notification);
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
                        oldItem.timestamp == newItem.timestamp &&
                        (oldItem.type == null ? newItem.type == null : oldItem.type.equals(newItem.type));
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
            String title = item.title;
            String content = item.content;

            // Apply local translations based on eventKey or type to handle server strings with no accents
            if ("birthday_reward".equals(item.eventKey)) {
                title = context.getString(R.string.notif_birthday_title);
                content = context.getString(R.string.notif_birthday_content);
            } else if ("order".equals(item.type)) {
                if (title != null && title.toLowerCase().startsWith("don hang")) {
                    String orderCode = title.substring(title.lastIndexOf(" ") + 1);
                    title = context.getString(R.string.notif_order_title_prefix) + orderCode;
                }
                if (content != null && content.toLowerCase().startsWith("trang thai hien tai")) {
                    String lowerContent = content.toLowerCase();
                    int statusResId = -1;
                    if (lowerContent.contains("cho xac nhan")) statusResId = R.string.status_pending;
                    else if (lowerContent.contains("da xac nhan")) statusResId = R.string.status_confirmed;
                    else if (lowerContent.contains("dang xu ly")) statusResId = R.string.status_processing;
                    else if (lowerContent.contains("da dong goi")) statusResId = R.string.status_packed;
                    else if (lowerContent.contains("dang giao")) statusResId = R.string.status_shipping;
                    else if (lowerContent.contains("da giao")) statusResId = R.string.status_delivered;
                    else if (lowerContent.contains("hoan thanh")) statusResId = R.string.status_completed;
                    else if (lowerContent.contains("da huy")) statusResId = R.string.status_cancelled;

                    if (statusResId != -1) {
                        content = context.getString(R.string.notif_order_status_update, context.getString(statusResId).toLowerCase());
                    }
                }
            }

            binding.tvTitle.setText(title);
            binding.tvContent.setText(content);

            // Relative Time formatting
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            binding.tvTime.setText(relativeTime);

            // Visual properties based on read/unread
            if (item.isRead) {
                binding.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
                binding.viewUnreadDot.setVisibility(View.GONE);
                binding.btnMarkUnread.setVisibility(View.VISIBLE);
            } else {
                binding.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                binding.viewUnreadDot.setVisibility(View.VISIBLE);
                binding.btnMarkUnread.setVisibility(View.GONE);
            }

            // Background color based on type
            int bgColor;
            int typeColor;
            if ("order".equals(item.type)) {
                bgColor = ContextCompat.getColor(context, R.color.bg_notif_order);
                typeColor = ContextCompat.getColor(context, R.color.notif_order);
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_cart);
            } else if ("account".equals(item.type)) {
                bgColor = ContextCompat.getColor(context, R.color.bg_notif_account);
                typeColor = ContextCompat.getColor(context, R.color.notif_account);
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_account);
            } else {
                bgColor = ContextCompat.getColor(context, R.color.bg_notif_other);
                typeColor = ContextCompat.getColor(context, R.color.notif_other);
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_bell);
            }
            binding.cardNotification.setCardBackgroundColor(bgColor);

            // soft background for icon container
            binding.cardIconContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            binding.ivNotificationIcon.setImageTintList(ColorStateList.valueOf(typeColor));
            binding.btnMarkUnread.setImageTintList(ColorStateList.valueOf(typeColor));
            binding.viewUnreadDot.setBackgroundTintList(ColorStateList.valueOf(typeColor));
            binding.viewUnreadDot.setClickable(true);
            binding.viewUnreadDot.setFocusable(true);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });

            View.OnClickListener showActionPopup = v -> showMarkUnreadPopup(v, item, listener);
            binding.viewUnreadDot.setOnClickListener(showActionPopup);
            binding.btnMarkUnread.setOnClickListener(showActionPopup);
        }

        private void showMarkUnreadPopup(View anchor, NotificationDto item, OnNotificationClickListener listener) {
            View popupView = LayoutInflater.from(context).inflate(R.layout.popup_notification_action, null, false);
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(dpToPx(8));

            TextView action = popupView.findViewById(R.id.tvPopupAction);
            action.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (listener != null) {
                    listener.onMarkUnreadClick(item);
                }
            });

            popupView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int xOffset = -popupView.getMeasuredWidth() + anchor.getWidth();
            popupWindow.showAsDropDown(anchor, xOffset, dpToPx(6));
        }

        private int dpToPx(int dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
    }
}
