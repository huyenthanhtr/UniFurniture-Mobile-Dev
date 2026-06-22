package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.databinding.ItemCategoryBinding;
import com.unifurniture.mobile.util.CategoryImageHelper;

public class CategoryAdapter extends ListAdapter<CategoryDto, CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onClick(CategoryDto category);
    }

    private final String serverHost;
    private final OnCategoryClickListener listener;

    public CategoryAdapter(String serverHost, OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.serverHost = serverHost;
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
                    // Trả về false để buộc adapter luôn vẽ lại ô, áp dụng logic ảnh mới từ CategoryImageHelper
                    return false;
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
        holder.bind(getItem(position), serverHost, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        ViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CategoryDto category, String serverHost, OnCategoryClickListener listener) {
            binding.tvCategoryName.setText(category.name);

            // Ưu tiên lấy link ảnh từ server hoặc CDN để có chất lượng/tỉ lệ tốt nhất
            String imageUrl = CategoryImageHelper.resolveNetworkUrl(category, serverHost);
            int placeholderRes = CategoryImageHelper.resolveDrawableRes(category);

            Glide.with(binding.getRoot())
                    .load(imageUrl != null ? imageUrl : placeholderRes)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .centerCrop() 
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(binding.ivCategory);

            binding.getRoot().setOnClickListener(v -> listener.onClick(category));
        }
    }
}
