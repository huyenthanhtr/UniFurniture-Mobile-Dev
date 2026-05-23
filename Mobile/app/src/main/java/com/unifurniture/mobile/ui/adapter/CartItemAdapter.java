package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CartItemDto;
import com.unifurniture.mobile.databinding.ItemCartBinding;
import com.unifurniture.mobile.util.FormatUtil;

public class CartItemAdapter extends ListAdapter<CartItemDto, CartItemAdapter.ViewHolder> {

    public interface OnQuantityChangeListener {
        void onChange(CartItemDto item, int newQuantity);
    }

    public interface OnRemoveListener {
        void onRemove(CartItemDto item);
    }

    private final OnQuantityChangeListener quantityListener;
    private final OnRemoveListener removeListener;

    public CartItemAdapter(OnQuantityChangeListener quantityListener, OnRemoveListener removeListener) {
        super(DIFF_CALLBACK);
        this.quantityListener = quantityListener;
        this.removeListener = removeListener;
    }

    private static final DiffUtil.ItemCallback<CartItemDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CartItemDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartItemDto a, @NonNull CartItemDto b) {
                    return a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull CartItemDto a, @NonNull CartItemDto b) {
                    return a.id.equals(b.id) &&
                            java.util.Objects.equals(a.quantity, b.quantity);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), quantityListener, removeListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartBinding binding;

        ViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CartItemDto item, OnQuantityChangeListener quantityListener,
                  OnRemoveListener removeListener) {
            // Product info
            if (item.product != null) {
                binding.tvName.setText(item.product.name);
                Glide.with(binding.getRoot())
                        .load(item.product.getImageUrl())
                        .placeholder(R.drawable.placeholder_product)
                        .centerCrop()
                        .into(binding.ivProduct);
            }
            if (item.variant != null && item.variant.color != null) {
                binding.tvVariant.setText("Màu: " + item.variant.color);
                binding.tvVariant.setVisibility(View.VISIBLE);
            }

            binding.tvPrice.setText(FormatUtil.formatCurrency(item.price));
            binding.tvQuantity.setText(String.valueOf(item.quantity != null ? item.quantity : 1));

            binding.btnMinus.setOnClickListener(v -> {
                int qty = item.quantity != null ? item.quantity : 1;
                quantityListener.onChange(item, qty - 1);
            });
            binding.btnPlus.setOnClickListener(v -> {
                int qty = item.quantity != null ? item.quantity : 1;
                quantityListener.onChange(item, qty + 1);
            });
            binding.btnRemove.setOnClickListener(v -> removeListener.onRemove(item));
        }
    }
}
