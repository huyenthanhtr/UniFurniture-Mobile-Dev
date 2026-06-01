package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CollectionDto;
import com.unifurniture.mobile.databinding.ItemCollectionBinding;
import java.util.Objects;

public class CollectionAdapter extends ListAdapter<CollectionDto, CollectionAdapter.ViewHolder> {

    public interface OnCollectionClickListener {
        void onClick(CollectionDto collection);
    }

    private final String serverHost;
    private final OnCollectionClickListener listener;

    public CollectionAdapter(String serverHost, OnCollectionClickListener listener) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CollectionDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CollectionDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull CollectionDto a, @NonNull CollectionDto b) {
                    return Objects.equals(a.id, b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull CollectionDto a, @NonNull CollectionDto b) {
                    return Objects.equals(a.id, b.id);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCollectionBinding binding = ItemCollectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), serverHost, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCollectionBinding binding;

        ViewHolder(ItemCollectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CollectionDto collection, String serverHost,
                  OnCollectionClickListener listener) {
            binding.tvCollectionName.setText(collection.name);
            String url = collection.bannerUrl != null
                    ? collection.bannerUrl.replace("http://localhost:3000", serverHost)
                    : null;
            Glide.with(binding.getRoot())
                    .load(url)
                    .placeholder(R.drawable.placeholder_product)
                    .centerCrop()
                    .into(binding.ivCollection);
            binding.getRoot().setOnClickListener(v -> listener.onClick(collection));
        }
    }
}
