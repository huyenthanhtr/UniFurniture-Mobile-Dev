package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.databinding.ItemReviewBinding;

public class ReviewAdapter extends ListAdapter<ReviewDto, ReviewAdapter.ViewHolder> {

    private final String serverHost;

    public ReviewAdapter(String serverHost) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
    }

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
        holder.bind(getItem(position), serverHost);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewBinding binding;

        ViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReviewDto review, String serverHost) {
            binding.tvCustomerName.setText(review.customerName != null ? review.customerName : itemView.getContext().getString(R.string.guest_customer));
            binding.tvContent.setText(review.content != null ? review.content : "");
            binding.ratingBar.setRating(review.rating != null ? review.rating : 0);
            binding.tvDate.setText(review.createdAt != null ?
                    review.createdAt.substring(0, Math.min(10, review.createdAt.length())) : "");

            // Review images
            if (review.images != null && !review.images.isEmpty()) {
                binding.scrollImages.setVisibility(View.VISIBLE);
                binding.layoutImages.removeAllViews();
                int sizePx = dpToPx(80);
                int marginPx = dpToPx(8);
                int cornerPx = dpToPx(6);
                for (String url : review.images) {
                    String imageUrl = url.replace("http://localhost:3000", serverHost);
                    ImageView iv = new ImageView(binding.getRoot().getContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                    lp.setMarginEnd(marginPx);
                    iv.setLayoutParams(lp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(binding.getRoot().getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_product)
                            .error(R.drawable.placeholder_product)
                            .apply(new RequestOptions().transform(
                                    new MultiTransformation<>(new CenterCrop(), new RoundedCorners(cornerPx))))
                            .transition(DrawableTransitionOptions.withCrossFade(150))
                            .into(iv);
                    binding.layoutImages.addView(iv);
                }
            } else {
                binding.scrollImages.setVisibility(View.GONE);
            }

            // Admin reply
            if (review.reply != null && review.reply.content != null
                    && !review.reply.content.isEmpty()) {
                binding.layoutReply.setVisibility(View.VISIBLE);
                binding.tvReplyContent.setText(review.reply.content);
                if (review.reply.repliedAt != null && review.reply.repliedAt.length() >= 10) {
                    binding.tvReplyDate.setText(review.reply.repliedAt.substring(0, 10));
                }
            } else {
                binding.layoutReply.setVisibility(View.GONE);
            }
        }

        private int dpToPx(int dp) {
            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}
