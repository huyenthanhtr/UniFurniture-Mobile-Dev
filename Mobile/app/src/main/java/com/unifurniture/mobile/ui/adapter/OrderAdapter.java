package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.databinding.ItemOrderBinding;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.util.FormatUtil;

public class OrderAdapter extends ListAdapter<OrderDto, OrderAdapter.ViewHolder> {

    public OrderAdapter() { super(DIFF_CALLBACK); }

    private static final DiffUtil.ItemCallback<OrderDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<OrderDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull OrderDto a, @NonNull OrderDto b) {
                    return a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull OrderDto a, @NonNull OrderDto b) {
                    return a.id.equals(b.id) &&
                            java.util.Objects.equals(a.status, b.status);
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
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderBinding binding;

        ViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderDto order) {
            binding.tvOrderId.setText(binding.getRoot().getContext().getString(R.string.str_order_code_format, order.getDisplayCode()));
            binding.tvStatus.setText(order.getStatusLabel());
            binding.tvTotal.setText(FormatUtil.formatCurrency(order.totalAmount));
            binding.tvDate.setText(order.getDisplayDate());
        }
    }
}
