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
import com.unifurniture.mobile.ui.common.ReviewImageViewerDialogFragment;
import com.unifurniture.mobile.ui.common.ReviewVideoViewerDialogFragment;
import com.unifurniture.mobile.util.LanguageHelper;
import com.unifurniture.mobile.util.ReviewTranslator;

public class ReviewAdapter extends ListAdapter<ReviewDto, ReviewAdapter.ViewHolder> {

    private final String serverHost;
    private final androidx.fragment.app.FragmentManager fragmentManager;

    public ReviewAdapter(String serverHost, androidx.fragment.app.FragmentManager fragmentManager) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
        this.fragmentManager = fragmentManager;
    }

    private static final DiffUtil.ItemCallback<ReviewDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReviewDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReviewDto a, @NonNull ReviewDto b) {
                    return a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull ReviewDto a, @NonNull ReviewDto b) {
                    return a.id.equals(b.id)
                            && a.showingTranslation == b.showingTranslation
                            && java.util.Objects.equals(a.content, b.content)
                            && java.util.Objects.equals(a.translatedContent, b.translatedContent);
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
        holder.bind(getItem(position), serverHost, this, fragmentManager);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewBinding binding;

        ViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReviewDto review, String serverHost, ReviewAdapter adapter, androidx.fragment.app.FragmentManager fragmentManager) {
            binding.tvCustomerName.setText(review.customerName != null ? review.customerName : itemView.getContext().getString(R.string.guest_customer));
            bindContentAndToggle(review, adapter);
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
                    final int clickedIndex = binding.layoutImages.getChildCount();
                    iv.setOnClickListener(v -> {
                        java.util.List<String> normalizedUrls = new java.util.ArrayList<>();
                        for (String image : review.images) {
                            normalizedUrls.add(image.replace("http://localhost:3000", serverHost));
                        }
                        ReviewImageViewerDialogFragment.newInstance(normalizedUrls, clickedIndex)
                                .show(fragmentManager, "review_image_viewer");
                    });
                    binding.layoutImages.addView(iv);
                }
            } else {
                binding.scrollImages.setVisibility(View.GONE);
            }

            if (review.videos != null && !review.videos.isEmpty()) {
                binding.scrollVideos.setVisibility(View.VISIBLE);
                binding.layoutVideos.removeAllViews();
                int widthPx = dpToPx(120);
                int heightPx = dpToPx(80);
                int marginPx = dpToPx(8);
                int cornerPx = dpToPx(6);
                for (String url : review.videos) {
                    String videoUrl = url.replace("http://localhost:3000", serverHost);
                    android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(binding.getRoot().getContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, heightPx);
                    lp.setMarginEnd(marginPx);
                    frameLayout.setLayoutParams(lp);

                    ImageView preview = new ImageView(binding.getRoot().getContext());
                    preview.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(binding.getRoot().getContext())
                            .load(videoUrl)
                            .placeholder(R.drawable.placeholder_product)
                            .error(R.drawable.placeholder_product)
                            .apply(new RequestOptions().transform(
                                    new MultiTransformation<>(new CenterCrop(), new RoundedCorners(cornerPx))))
                            .transition(DrawableTransitionOptions.withCrossFade(150))
                            .into(preview);

                    ImageView playBadge = new ImageView(binding.getRoot().getContext());
                    android.widget.FrameLayout.LayoutParams badgeParams = new android.widget.FrameLayout.LayoutParams(
                            dpToPx(28), dpToPx(28), android.view.Gravity.CENTER);
                    playBadge.setLayoutParams(badgeParams);
                    playBadge.setImageResource(android.R.drawable.ic_media_play);
                    playBadge.setColorFilter(android.graphics.Color.WHITE);

                    frameLayout.addView(preview);
                    frameLayout.addView(playBadge);
                    frameLayout.setOnClickListener(v -> ReviewVideoViewerDialogFragment
                            .newInstance(videoUrl)
                            .show(fragmentManager, "review_video_viewer"));
                    binding.layoutVideos.addView(frameLayout);
                }
            } else {
                binding.scrollVideos.setVisibility(View.GONE);
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

        /**
         * Show the review text (original or translated) and the grey Translate / See Original toggle.
         * The original text is always preserved on the ReviewDto; translation happens on-device on
         * demand and is cached on the item so re-binding (and toggling back) is instant.
         */
        private void bindContentAndToggle(ReviewDto review, ReviewAdapter adapter) {
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
                    // Toggle back to the original.
                    review.showingTranslation = false;
                    adapter.notifyItemChanged(pos);
                    return;
                }
                if (review.translatedContent != null) {
                    // Already translated once — just flip.
                    review.showingTranslation = true;
                    adapter.notifyItemChanged(pos);
                    return;
                }
                // First time: translate into the current UI language on-device.
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
                        if (binding != null) binding.tvTranslateToggle.setText(R.string.review_translate);
                        com.unifurniture.mobile.util.ToastUtil.error(
                                itemView.getContext(), R.string.review_translate_failed);
                    }
                });
            });
        }

        private int dpToPx(int dp) {
            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}
