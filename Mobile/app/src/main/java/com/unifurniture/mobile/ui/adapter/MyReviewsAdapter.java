package com.unifurniture.mobile.ui.adapter;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ReviewDto;
import com.unifurniture.mobile.databinding.ItemMyReviewBinding;
import com.unifurniture.mobile.util.LanguageHelper;
import com.unifurniture.mobile.util.ReviewTranslator;
import com.unifurniture.mobile.util.ToastUtil;

public class MyReviewsAdapter extends ListAdapter<ReviewDto, MyReviewsAdapter.ViewHolder> {

    public interface OnReviewClickListener {
        void onOpenProduct(ReviewDto review);
    }

    private final String serverHost;
    private final OnReviewClickListener listener;

    public MyReviewsAdapter(String serverHost, OnReviewClickListener listener) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ReviewDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<ReviewDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull ReviewDto oldItem, @NonNull ReviewDto newItem) {
            return java.util.Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ReviewDto oldItem, @NonNull ReviewDto newItem) {
            return java.util.Objects.equals(oldItem.getContent(), newItem.getContent()) &&
                   java.util.Objects.equals(oldItem.getRating(), newItem.getRating()) &&
                   java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                   java.util.Objects.equals(oldItem.translatedContent, newItem.translatedContent) &&
                   oldItem.showingTranslation == newItem.showingTranslation;
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
        holder.bind(getItem(position), serverHost, listener, this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMyReviewBinding binding;

        ViewHolder(ItemMyReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReviewDto review, String serverHost, OnReviewClickListener listener, MyReviewsAdapter adapter) {
            binding.ratingBar.setRating(review.getRating() != null ? review.getRating() : 0);
            binding.tvDate.setText(formatShortDate(review.getCreatedAt()));
            binding.tvProductName.setText(nonEmpty(review.getProductName(), itemView.getContext().getString(R.string.product_default_name)));
            binding.tvOrderCode.setText(nonEmpty(review.getOrderCode(), "-"));
            bindStatus(review.getStatus());
            bindContentAndToggle(review, adapter);
            bindProductImage(review.getProductImageUrl(), serverHost);
            bindImages(review.getImages(), serverHost);
            bindVideos(review.getVideos());

            if (review.getReply() != null && review.getReply().getContent() != null && !review.getReply().getContent().isEmpty()) {
                binding.llReply.setVisibility(View.VISIBLE);
                binding.tvReplyContent.setText(review.getReply().getContent());
                binding.tvReplyDate.setText(formatShortDate(review.getReply().getRepliedAt()));
            } else {
                binding.llReply.setVisibility(View.GONE);
            }

            binding.btnViewProduct.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpenProduct(review);
                }
            });
        }

        private void bindProductImage(String rawUrl, String serverHost) {
            String imageUrl = normalizeUrl(rawUrl, serverHost);
            Glide.with(binding.getRoot())
                    .load(imageUrl.isEmpty() ? R.drawable.placeholder_product : imageUrl)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .apply(new RequestOptions().transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(dpToPx(10)))))
                    .transition(DrawableTransitionOptions.withCrossFade(150))
                    .into(binding.ivProductImage);
        }

        private void bindImages(java.util.List<String> images, String serverHost) {
            if (images == null || images.isEmpty()) {
                binding.scrollImages.setVisibility(View.GONE);
                return;
            }
            binding.scrollImages.setVisibility(View.VISIBLE);
            binding.layoutImages.removeAllViews();
            int sizePx = dpToPx(72);
            int marginPx = dpToPx(8);
            int cornerPx = dpToPx(8);
            for (String raw : images) {
                String imageUrl = normalizeUrl(raw, serverHost);
                ImageView iv = new ImageView(binding.getRoot().getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMarginEnd(marginPx);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(binding.getRoot())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_product)
                        .error(R.drawable.placeholder_product)
                        .apply(new RequestOptions().transform(
                                new MultiTransformation<>(new CenterCrop(), new RoundedCorners(cornerPx))))
                        .transition(DrawableTransitionOptions.withCrossFade(120))
                        .into(iv);
                iv.setOnClickListener(v -> openUrl(imageUrl));
                binding.layoutImages.addView(iv);
            }
        }

        private void bindVideos(java.util.List<String> videos) {
            if (videos == null || videos.isEmpty()) {
                binding.layoutVideos.setVisibility(View.GONE);
                binding.layoutVideoButtons.removeAllViews();
                return;
            }
            binding.layoutVideos.setVisibility(View.VISIBLE);
            binding.layoutVideoButtons.removeAllViews();
            int index = 1;
            for (String videoUrl : videos) {
                MaterialButton button = new MaterialButton(binding.getRoot().getContext(), null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                button.setText(itemView.getContext().getString(R.string.review_video_label, index));
                button.setInsetTop(0);
                button.setInsetBottom(0);
                button.setMinHeight(dpToPx(36));
                button.setOnClickListener(v -> openUrl(videoUrl));
                binding.layoutVideoButtons.addView(button);
                index += 1;
            }
        }

        private void bindStatus(String status) {
            String normalized = String.valueOf(status == null ? "" : status).trim().toLowerCase();
            int textColor;
            int bgColor;
            int strokeColor;
            int labelRes;
            switch (normalized) {
                case "approved":
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.status_completed_text);
                    bgColor = ContextCompat.getColor(itemView.getContext(), R.color.status_completed_bg);
                    strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.status_completed_stroke);
                    labelRes = R.string.review_status_approved;
                    break;
                case "rejected":
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.status_cancelled_text);
                    bgColor = ContextCompat.getColor(itemView.getContext(), R.color.status_cancelled_bg);
                    strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.status_cancelled_stroke);
                    labelRes = R.string.review_status_rejected;
                    break;
                default:
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.status_pending_text);
                    bgColor = ContextCompat.getColor(itemView.getContext(), R.color.status_pending_bg);
                    strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.status_pending_stroke);
                    labelRes = R.string.review_status_pending;
                    break;
            }
            binding.tvStatus.setText(labelRes);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(dpToPx(999));
            drawable.setColor(bgColor);
            drawable.setStroke(dpToPx(1), strokeColor);
            binding.tvStatus.setBackground(drawable);
            binding.tvStatus.setTextColor(textColor);
        }

        private void bindContentAndToggle(ReviewDto review, MyReviewsAdapter adapter) {
            String original = review.content != null ? review.content : "";
            boolean hasText = !original.trim().isEmpty();

            binding.tvContent.setText(review.showingTranslation && review.translatedContent != null
                    ? review.translatedContent : original);

            if (!hasText) {
                binding.tvTranslateToggle.setVisibility(View.GONE);
                return;
            }
            binding.tvTranslateToggle.setVisibility(View.VISIBLE);
            binding.tvTranslateToggle.setText(review.showingTranslation
                    ? R.string.review_see_original : R.string.review_translate);

            binding.tvTranslateToggle.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                if (review.showingTranslation) {
                    review.showingTranslation = false;
                    adapter.notifyItemChanged(pos);
                    return;
                }
                if (review.translatedContent != null) {
                    review.showingTranslation = true;
                    adapter.notifyItemChanged(pos);
                    return;
                }
                String target = LanguageHelper.getLanguage(itemView.getContext());
                binding.tvTranslateToggle.setText(R.string.review_translating);
                ReviewTranslator.translate(original, target, new ReviewTranslator.Callback() {
                    @Override
                    public void onResult(String translatedText) {
                        review.translatedContent = translatedText;
                        review.showingTranslation = true;
                        int p = getBindingAdapterPosition();
                        adapter.notifyItemChanged(p == RecyclerView.NO_POSITION ? pos : p);
                    }

                    @Override
                    public void onError() {
                        binding.tvTranslateToggle.setText(R.string.review_translate);
                        ToastUtil.error(itemView.getContext(), R.string.review_translate_failed);
                    }
                });
            });
        }

        private String formatShortDate(String raw) {
            if (raw == null || raw.trim().isEmpty()) return "";
            return raw.substring(0, Math.min(10, raw.length()));
        }

        private String nonEmpty(String value, String fallback) {
            return value != null && !value.trim().isEmpty() ? value : fallback;
        }

        private String normalizeUrl(String rawUrl, String serverHost) {
            String value = rawUrl == null ? "" : rawUrl.trim();
            if (value.isEmpty()) return "";
            return value.replace("http://localhost:3000", serverHost);
        }

        private void openUrl(String value) {
            if (value == null || value.trim().isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
            itemView.getContext().startActivity(intent);
        }

        private int dpToPx(int dp) {
            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}
