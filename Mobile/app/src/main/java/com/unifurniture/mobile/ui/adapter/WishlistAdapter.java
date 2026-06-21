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
import com.unifurniture.mobile.data.model.WishlistItemDto;
import com.unifurniture.mobile.databinding.ItemWishlistBinding;
import com.unifurniture.mobile.util.FormatUtil;

public class WishlistAdapter extends ListAdapter<WishlistItemDto, WishlistAdapter.ViewHolder> {

    public interface OnWishlistClickListener {
        void onClick(WishlistItemDto item);
        void onRemove(WishlistItemDto item);
    }

    private final String serverHost;
    private final OnWishlistClickListener listener;

    public WishlistAdapter(String serverHost, OnWishlistClickListener listener) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<WishlistItemDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WishlistItemDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull WishlistItemDto a, @NonNull WishlistItemDto b) {
                    return java.util.Objects.equals(a.id, b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull WishlistItemDto a, @NonNull WishlistItemDto b) {
                    return java.util.Objects.equals(a.id, b.id);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWishlistBinding binding = ItemWishlistBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), serverHost, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemWishlistBinding binding;

        ViewHolder(ItemWishlistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WishlistItemDto item, String serverHost, OnWishlistClickListener listener) {
            ProductDto product = item.getProduct();
            if (product != null) {
                binding.tvName.setText(product.name != null ? product.name : "");
                binding.tvPrice.setText(FormatUtil.formatCurrency(product.minPrice));
                
                String imageUrl = com.unifurniture.mobile.util.CategoryImageHelper.resolveProductUrl(product, serverHost);

                Glide.with(binding.getRoot())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_product)
                        .error(R.drawable.placeholder_product)
                        .centerCrop()
                        .into(binding.ivProduct);
                
                binding.getRoot().setOnClickListener(v -> listener.onClick(item));
            } else {
                binding.tvName.setText("");
                binding.tvPrice.setText(FormatUtil.formatCurrency(0));
                binding.ivProduct.setImageResource(R.drawable.placeholder_product);
            }
            
            binding.btnRemove.setOnClickListener(v -> listener.onRemove(item));
        }
    }
}
