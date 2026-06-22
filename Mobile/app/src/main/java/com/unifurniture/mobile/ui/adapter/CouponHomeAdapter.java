package com.unifurniture.mobile.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CouponDto;
import com.unifurniture.mobile.databinding.ItemCouponHomeBinding;
import com.unifurniture.mobile.util.FormatUtil;

public class CouponHomeAdapter extends ListAdapter<CouponDto, CouponHomeAdapter.ViewHolder> {

    public CouponHomeAdapter() {
        super(new DiffUtil.ItemCallback<CouponDto>() {
            @Override
            public boolean areItemsTheSame(@NonNull CouponDto a, @NonNull CouponDto b) {
                return a.code != null && a.code.equals(b.code);
            }
            @Override
            public boolean areContentsTheSame(@NonNull CouponDto a, @NonNull CouponDto b) {
                return a.discountValue == b.discountValue && a.minOrderValue == b.minOrderValue;
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCouponHomeBinding binding = ItemCouponHomeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCouponHomeBinding binding;

        ViewHolder(ItemCouponHomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CouponDto coupon) {
            Context ctx = binding.getRoot().getContext();

            // Compact label for the left accent tab
            if ("percent".equals(coupon.discountType)) {
                binding.tvDiscountValue.setText((int) coupon.discountValue + "%");
            } else {
                long val = (long) coupon.discountValue;
                String compact;
                if (val >= 1_000_000) {
                    compact = String.format(java.util.Locale.US, "%.1fM", val / 1_000_000.0);
                } else {
                    compact = (val / 1000) + "K";
                }
                binding.tvDiscountValue.setText(compact);
            }

            String code = coupon.code != null ? coupon.code : "";
            binding.tvCouponCode.setText(code);

            // Copy code on icon click or card tap
            android.view.View.OnClickListener copyListener = v -> {
                if (code.isEmpty()) return;
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("coupon_code", code));
                    Toast.makeText(ctx, ctx.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }
            };
            binding.ivCopyCode.setOnClickListener(copyListener);
            binding.getRoot().setOnClickListener(copyListener);

            if (coupon.minOrderValue > 0) {
                binding.tvMinOrder.setText(
                        ctx.getString(R.string.coupon_min_order,
                                FormatUtil.formatCurrency(coupon.minOrderValue)));
            } else {
                binding.tvMinOrder.setText(ctx.getString(R.string.coupon_no_min));
            }

            if (coupon.endAt != null && !coupon.endAt.isEmpty()) {
                String dateOnly = coupon.endAt.length() >= 10 ? coupon.endAt.substring(0, 10) : coupon.endAt;
                binding.tvExpiry.setText(ctx.getString(R.string.voucher_expiry, dateOnly));
                binding.tvExpiry.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvExpiry.setVisibility(android.view.View.GONE);
            }
        }
    }
}
