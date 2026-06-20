package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.databinding.ItemCategoryBinding;

public class CategoryAdapter extends ListAdapter<CategoryDto, CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onClick(CategoryDto category);
    }

    private final OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CategoryDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CategoryDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryDto a, @NonNull CategoryDto b) {
                    return a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull CategoryDto a, @NonNull CategoryDto b) {
                    return a.id.equals(b.id);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        ViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CategoryDto category, OnCategoryClickListener listener) {
            binding.tvCategoryName.setText(category.name);
            Glide.with(binding.getRoot())
                    .load(category.imageUrl)
                    .placeholder(R.drawable.placeholder_category)
                    .circleCrop()
                    .into(binding.ivCategory);
            binding.getRoot().setOnClickListener(v -> listener.onClick(category));
        }
    }
}
