package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.databinding.ItemSearchHistoryBinding;

public class SearchHistoryAdapter extends ListAdapter<String, SearchHistoryAdapter.ViewHolder> {

    public interface OnItemListener {
        void onQueryClick(String query);
        void onDeleteClick(String query);
    }

    private final OnItemListener listener;

    public SearchHistoryAdapter(OnItemListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<String>() {
                @Override
                public boolean areItemsTheSame(@NonNull String a, @NonNull String b) {
                    return a.equals(b);
                }
                @Override
                public boolean areContentsTheSame(@NonNull String a, @NonNull String b) {
                    return a.equals(b);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchHistoryBinding binding = ItemSearchHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchHistoryBinding binding;

        ViewHolder(ItemSearchHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String query, OnItemListener listener) {
            binding.tvQuery.setText(query);
            binding.getRoot().setOnClickListener(v -> listener.onQueryClick(query));
            binding.btnDelete.setOnClickListener(v -> listener.onDeleteClick(query));
        }
    }
}
