package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.databinding.ItemMyReviewBinding;

public class MyReviewsAdapter extends ListAdapter<ReviewDto, MyReviewsAdapter.ViewHolder> {

    public MyReviewsAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ReviewDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<ReviewDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull ReviewDto oldItem, @NonNull ReviewDto newItem) {
            return java.util.Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ReviewDto oldItem, @NonNull ReviewDto newItem) {
            return java.util.Objects.equals(oldItem.getContent(), newItem.getContent()) &&
                   java.util.Objects.equals(oldItem.getRating(), newItem.getRating());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMyReviewBinding binding = ItemMyReviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMyReviewBinding binding;

        ViewHolder(ItemMyReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReviewDto review) {
            binding.ratingBar.setRating(review.getRating() != null ? review.getRating() : 0);
            binding.tvContent.setText(review.getContent() != null ? review.getContent() : "");
            binding.tvDate.setText(review.getCreatedAt() != null ? review.getCreatedAt().substring(0, Math.min(10, review.getCreatedAt().length())) : "");

            if (review.getReply() != null && review.getReply().getContent() != null && !review.getReply().getContent().isEmpty()) {
                binding.llReply.setVisibility(View.VISIBLE);
                binding.tvReplyContent.setText(review.getReply().getContent());
            } else {
                binding.llReply.setVisibility(View.GONE);
            }
        }
    }
}
