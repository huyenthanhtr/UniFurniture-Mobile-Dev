package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.databinding.ItemProductCardBinding;
import com.unifurniture.mobile.util.FormatUtil;

public class ProductCardAdapter extends ListAdapter<ProductDto, ProductCardAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onClick(ProductDto product);
    }

    private final OnProductClickListener listener;
    private int columns = 2;

    public ProductCardAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setColumns(int columns) {
        if (this.columns == columns) return;
        this.columns = columns;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<ProductDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProductDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull ProductDto a, @NonNull ProductDto b) {
                    return a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull ProductDto a, @NonNull ProductDto b) {
                    return a.id.equals(b.id);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductCardBinding binding = ItemProductCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.itemView.setLayoutParams(lp);
        }
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductCardBinding binding;

        ViewHolder(ItemProductCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProductDto product, OnProductClickListener listener) {
            binding.tvName.setText(product.name);
            binding.tvPrice.setText(FormatUtil.formatCurrency(product.minPrice));

            String badge = FormatUtil.discountBadge(product.minPrice, product.compareAtPrice);
            if (badge != null) {
                binding.tvDiscount.setText(badge);
                binding.tvDiscount.setVisibility(android.view.View.VISIBLE);
                binding.tvOriginalPrice.setText(FormatUtil.formatCurrency(product.compareAtPrice));
                binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvOriginalPrice.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvDiscount.setVisibility(android.view.View.GONE);
                binding.tvOriginalPrice.setVisibility(android.view.View.GONE);
            }

            Glide.with(binding.getRoot())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_product)
                    .centerCrop()
                    .into(binding.ivProduct);

            binding.getRoot().setOnClickListener(v -> listener.onClick(product));
        }
    }
}
