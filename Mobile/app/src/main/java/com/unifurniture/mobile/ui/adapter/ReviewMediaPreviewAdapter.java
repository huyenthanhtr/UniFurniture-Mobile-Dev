package com.unifurniture.mobile.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.ItemReviewMediaPreviewBinding;

import java.util.List;

public class ReviewMediaPreviewAdapter extends RecyclerView.Adapter<ReviewMediaPreviewAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public interface OnPreviewClickListener {
        void onPreviewClick(int position);
    }

    private final List<Uri> items;
    private final boolean videoMode;
    private final OnRemoveListener onRemoveListener;
    private final OnPreviewClickListener onPreviewClickListener;

    public ReviewMediaPreviewAdapter(List<Uri> items, boolean videoMode, OnRemoveListener onRemoveListener, OnPreviewClickListener onPreviewClickListener) {
        this.items = items;
        this.videoMode = videoMode;
        this.onRemoveListener = onRemoveListener;
        this.onPreviewClickListener = onPreviewClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewMediaPreviewBinding binding = ItemReviewMediaPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), videoMode, onRemoveListener, onPreviewClickListener);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewMediaPreviewBinding binding;

        ViewHolder(ItemReviewMediaPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Uri uri, boolean videoMode, OnRemoveListener onRemoveListener, OnPreviewClickListener onPreviewClickListener) {
            Glide.with(binding.getRoot().getContext())
                    .load(uri)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .centerCrop()
                    .into(binding.ivPreview);

            binding.ivVideoBadge.setVisibility(videoMode ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.ivPreview.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onPreviewClickListener != null) {
                    onPreviewClickListener.onPreviewClick(pos);
                }
            });
            binding.btnRemove.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onRemoveListener != null) {
                    onRemoveListener.onRemove(pos);
                }
            });
        }
    }
}
