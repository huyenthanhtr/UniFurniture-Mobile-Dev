package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.ItemRecentlyViewedBinding;
import com.unifurniture.mobile.util.RecentlyViewedManager.Item;

public class RecentlyViewedAdapter extends ListAdapter<Item, RecentlyViewedAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private final OnItemClickListener listener;

    public RecentlyViewedAdapter(OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<>() {
        @Override public boolean areItemsTheSame(@NonNull Item a, @NonNull Item b) {
            return a.id != null && a.id.equals(b.id);
        }
        @Override public boolean areContentsTheSame(@NonNull Item a, @NonNull Item b) {
            return java.util.Objects.equals(a.name, b.name)
                    && java.util.Objects.equals(a.imageUrl, b.imageUrl);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemRecentlyViewedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentlyViewedBinding binding;

        ViewHolder(ItemRecentlyViewedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item, OnItemClickListener listener) {
            binding.tvName.setText(item.name);
            Glide.with(binding.getRoot().getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder_product)
                    .centerCrop()
                    .into(binding.ivThumbnail);
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
