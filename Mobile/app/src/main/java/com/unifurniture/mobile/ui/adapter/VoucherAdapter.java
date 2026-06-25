package com.unifurniture.mobile.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.VoucherDto;
import com.unifurniture.mobile.databinding.ItemVoucherBinding;
import com.unifurniture.mobile.util.FormatUtil;

public class VoucherAdapter extends ListAdapter<VoucherDto, VoucherAdapter.ViewHolder> {

    public interface OnVoucherClickListener {
        void onVoucherClick(VoucherDto voucher);
    }

    private final double cartSubtotal;
    private final OnVoucherClickListener listener;

    public VoucherAdapter(double cartSubtotal, OnVoucherClickListener listener) {
        super(new DiffUtil.ItemCallback<VoucherDto>() {
            @Override
            public boolean areItemsTheSame(@NonNull VoucherDto oldItem, @NonNull VoucherDto newItem) {
                return oldItem.code.equals(newItem.code);
            }

            @Override
            public boolean areContentsTheSame(@NonNull VoucherDto oldItem, @NonNull VoucherDto newItem) {
                return oldItem.isUsed == newItem.isUsed &&
                        oldItem.name.equals(newItem.name) &&
                        oldItem.description.equals(newItem.description) &&
                        oldItem.discountValue == newItem.discountValue &&
                        oldItem.minOrderValue == newItem.minOrderValue;
            }
        });
        this.cartSubtotal = cartSubtotal;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVoucherBinding binding = ItemVoucherBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), cartSubtotal, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemVoucherBinding binding;
        private final Context context;

        public ViewHolder(@NonNull ItemVoucherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        public void bind(VoucherDto item, double subtotal, OnVoucherClickListener listener) {
            binding.tvVoucherCode.setText(item.code);
            binding.tvVoucherName.setText(item.name);
            binding.tvVoucherDescription.setText(item.description);
            binding.tvVoucherExpiry.setText(item.expirationDate);

            // Configure tag on the left sidebar
            if ("percent".equals(item.discountType)) {
                binding.tvStubText.setText(
                        context.getString(R.string.discount_percent_stub, (int) item.discountValue));
                binding.ivStubIcon.setImageResource(R.drawable.ic_products);
            } else {
                binding.tvStubText.setText(
                        context.getString(R.string.discount_fixed_stub, (int)(item.discountValue / 1000)));
                binding.ivStubIcon.setImageResource(R.drawable.ic_cart);
            }

            // Check eligibility against cart subtotal
            boolean isUsed = item.isUsed;
            boolean isEligible = subtotal >= item.minOrderValue;
            binding.getRoot().setAlpha(isUsed ? 0.55f : 1.0f);
            binding.layoutStub.setBackgroundColor(ContextCompat.getColor(context,
                    isUsed ? R.color.gray_500 : R.color.primary));
            binding.btnUse.setEnabled(!isUsed);
            binding.btnUse.setAlpha(isUsed ? 0.8f : 1.0f);
            binding.btnUse.setText(isUsed ? R.string.voucher_used_status : R.string.btn_use);
            binding.btnUse.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    isUsed ? R.color.gray_400 : R.color.primary)));
            
            if (isUsed) {
                binding.spacerCondition.setVisibility(View.GONE);
                binding.tvConditionWarning.setText(R.string.voucher_already_used);
                binding.tvConditionWarning.setTextColor(ContextCompat.getColor(context, R.color.gray_600));
                binding.tvConditionWarning.setVisibility(View.VISIBLE);
            } else if (isEligible) {
                binding.tvConditionWarning.setVisibility(View.GONE);
                binding.spacerCondition.setVisibility(View.VISIBLE);
            } else {
                binding.spacerCondition.setVisibility(View.GONE);
                double needed = item.minOrderValue - subtotal;
                binding.tvConditionWarning.setText(
                        context.getString(R.string.voucher_upsell, FormatUtil.formatCurrency(needed)));
                binding.tvConditionWarning.setTextColor(ContextCompat.getColor(context, R.color.discount_red));
                binding.tvConditionWarning.setVisibility(View.VISIBLE);
            }

            binding.btnUse.setOnClickListener(v -> {
                if (!item.isUsed && listener != null) {
                    listener.onVoucherClick(item);
                }
            });
        }
    }
}
