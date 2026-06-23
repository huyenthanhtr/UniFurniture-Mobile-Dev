package com.unifurniture.mobile.ui.adapter;

import android.content.Context;
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
import com.unifurniture.mobile.data.model.ProductDto;
import com.unifurniture.mobile.data.model.ProductVariantDto;
import com.unifurniture.mobile.databinding.ItemCartBinding;
import com.unifurniture.mobile.util.FormatUtil;

public class CartItemAdapter extends ListAdapter<CartItemDto, CartItemAdapter.ViewHolder> {

    public interface OnQuantityChangeListener {
        void onChange(CartItemDto item, int newQuantity);
    }

    public interface OnRemoveListener {
        void onRemove(CartItemDto item);
    }

    public interface OnVariantClickListener {
        void onVariantClick(CartItemDto item);
    }

    public interface OnItemClickListener {
        void onItemClick(CartItemDto item);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChange(CartItemDto item, boolean isSelected);
    }

    private final OnQuantityChangeListener quantityListener;
    private final OnRemoveListener removeListener;
    private final OnVariantClickListener variantClickListener;
    private final OnItemClickListener itemClickListener;
    private final OnSelectionChangeListener selectionChangeListener;
    private final String serverHost;

    public CartItemAdapter(String serverHost, OnQuantityChangeListener quantityListener, OnRemoveListener removeListener, OnVariantClickListener variantClickListener, OnItemClickListener itemClickListener, OnSelectionChangeListener selectionChangeListener) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
        this.quantityListener = quantityListener;
        this.removeListener = removeListener;
        this.variantClickListener = variantClickListener;
        this.itemClickListener = itemClickListener;
        this.selectionChangeListener = selectionChangeListener;
    }

    private static final DiffUtil.ItemCallback<CartItemDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CartItemDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartItemDto a, @NonNull CartItemDto b) {
                    return java.util.Objects.equals(a.id, b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull CartItemDto a, @NonNull CartItemDto b) {
                    return java.util.Objects.equals(a.id, b.id) &&
                            java.util.Objects.equals(a.quantity, b.quantity) &&
                            java.util.Objects.equals(a.getVariantId(), b.getVariantId()) &&
                            java.util.Objects.equals(a.getUnitPrice(), b.getUnitPrice()) &&
                            a.isSelected == b.isSelected;
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
        holder.bind(getItem(position), serverHost, quantityListener, removeListener, variantClickListener, itemClickListener, selectionChangeListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartBinding binding;

        ViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CartItemDto item, String serverHost, OnQuantityChangeListener quantityListener,
                  OnRemoveListener removeListener, OnVariantClickListener variantClickListener,
                  OnItemClickListener itemClickListener, OnSelectionChangeListener selectionChangeListener) {
            // Product info
            ProductDto product = item.getProduct();
            if (product != null) {
                binding.tvName.setText(product.name);
                Context context = binding.getRoot().getContext();
                if (context != null) {
                    String imageUrl = com.unifurniture.mobile.util.CategoryImageHelper.resolveProductUrl(product, serverHost);
                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_product)
                            .error(R.drawable.placeholder_product)
                            .centerCrop()
                            .into(binding.ivProduct);
                }
                
                binding.cardProductImage.setOnClickListener(v -> {
                    if (itemClickListener != null) itemClickListener.onItemClick(item);
                });
                binding.tvName.setOnClickListener(v -> {
                    if (itemClickListener != null) itemClickListener.onItemClick(item);
                });
            } else {
                binding.tvName.setText("");
                binding.ivProduct.setImageResource(R.drawable.placeholder_product);
                binding.cardProductImage.setOnClickListener(null);
                binding.tvName.setOnClickListener(null);
            }
            ProductVariantDto variant = item.getVariant();
            if (variant != null) {
                binding.tvVariant.setText(FormatUtil.getVariantLabel(variant));
                binding.tvVariant.setVisibility(View.VISIBLE);

                // Show Shopee-style pricing
                double unitPrice = item.getUnitPrice();
                Double compareAtPrice = variant.compareAtPrice;
                if (compareAtPrice != null && compareAtPrice > unitPrice) {
                    binding.tvOriginalPrice.setText(FormatUtil.formatCurrency(compareAtPrice));
                    binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                    
                    String badge = FormatUtil.discountBadge(unitPrice, compareAtPrice);
                    if (badge != null) {
                        binding.tvDiscountBadge.setText(badge);
                        binding.tvDiscountBadge.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvDiscountBadge.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvOriginalPrice.setVisibility(View.GONE);
                    binding.tvDiscountBadge.setVisibility(View.GONE);
                }
            } else {
                binding.tvVariant.setVisibility(View.GONE);
                binding.tvOriginalPrice.setVisibility(View.GONE);
                binding.tvDiscountBadge.setVisibility(View.GONE);
            }

            binding.tvPrice.setText(FormatUtil.formatCurrency(item.getUnitPrice()));
            binding.tvQuantity.setText(String.valueOf(item.quantity != null ? item.quantity : 1));

            binding.cbSelected.setOnCheckedChangeListener(null);
            binding.cbSelected.setChecked(item.isSelected);
            binding.cbSelected.setOnCheckedChangeListener((btn, checked) -> {
                item.isSelected = checked;
                if (selectionChangeListener != null) selectionChangeListener.onSelectionChange(item, checked);
            });

            binding.btnMinus.setOnClickListener(v -> {
                int qty = item.quantity != null ? item.quantity : 1;
                quantityListener.onChange(item, qty - 1);
            });
            binding.btnPlus.setOnClickListener(v -> {
                int qty = item.quantity != null ? item.quantity : 1;
                quantityListener.onChange(item, qty + 1);
            });
            binding.btnRemove.setOnClickListener(v -> removeListener.onRemove(item));
            binding.tvVariant.setOnClickListener(v -> {
                if (variantClickListener != null) {
                    variantClickListener.onVariantClick(item);
                }
            });
        }
    }
}
