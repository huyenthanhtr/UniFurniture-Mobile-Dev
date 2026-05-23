package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.databinding.ItemReviewBinding;

public class ReviewAdapter extends ListAdapter<ReviewDto, ReviewAdapter.ViewHolder> {

    public ReviewAdapter() { super(DIFF_CALLBACK); }

    private static final DiffUtil.ItemCallback<ReviewDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReviewDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReviewDto a, @NonNull ReviewDto b) {
                    return a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull ReviewDto a, @NonNull ReviewDto b) {
                    return a.id.equals(b.id);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewBinding binding;

        ViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReviewDto review) {
            binding.tvCustomerName.setText(review.customerName != null ? review.customerName : "Khách hàng");
            binding.tvContent.setText(review.content);
            binding.ratingBar.setRating(review.rating != null ? review.rating : 5);
            binding.tvDate.setText(review.createdAt != null ?
                    review.createdAt.substring(0, Math.min(10, review.createdAt.length())) : "");
        }
    }
}
