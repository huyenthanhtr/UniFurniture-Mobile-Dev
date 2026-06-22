package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.databinding.ItemOrderBinding;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.OrderStatusUi;

public class OrderAdapter extends ListAdapter<OrderDto, OrderAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(OrderDto order);
    }

    private final OnItemClickListener listener;

    public OrderAdapter(OnItemClickListener listener) { 
        super(DIFF_CALLBACK); 
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<OrderDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<OrderDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull OrderDto a, @NonNull OrderDto b) {
                    return java.util.Objects.equals(a.getId(), b.getId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull OrderDto a, @NonNull OrderDto b) {
                    return a.getId().equals(b.getId()) &&
                            java.util.Objects.equals(a.getStatus(), b.getStatus());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDto order = getItem(position);
        holder.bind(order);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(order);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderBinding binding;

        ViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderDto order) {
            String displayCode = order.getOrderCode();
            if (displayCode == null || displayCode.trim().isEmpty()) {
                String id = order.getId();
                displayCode = id != null && !id.isEmpty()
                        ? "#" + id.substring(Math.max(0, id.length() - 8))
                        : "-";
            }
            binding.tvOrderId.setText(binding.getRoot().getContext().getString(com.unifurniture.mobile.R.string.order_number_format, displayCode));
            OrderStatusUi.applyBadge(binding.tvStatus, order.getStatus());
            binding.tvTotal.setText(FormatUtil.formatCurrency(order.getTotalAmount()));
            binding.tvDate.setText(order.getCreatedAt() != null ?
                    order.getCreatedAt().substring(0, Math.min(10, order.getCreatedAt().length())) : "");
        }
    }
}
