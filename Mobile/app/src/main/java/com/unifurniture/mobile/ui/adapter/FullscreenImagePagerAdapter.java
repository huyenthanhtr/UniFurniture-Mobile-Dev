package com.unifurniture.mobile.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.unifurniture.mobile.R;

import java.util.List;

public class FullscreenImagePagerAdapter extends RecyclerView.Adapter<FullscreenImagePagerAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public FullscreenImagePagerAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PhotoView photoView = new PhotoView(context);
        photoView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        photoView.setBackgroundColor(android.graphics.Color.BLACK);
        return new ViewHolder(photoView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(context)
                .load(imageUrls.get(position))
                .placeholder(R.drawable.placeholder_product)
                .error(R.drawable.placeholder_product)
                .fitCenter()
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageUrls == null ? 0 : imageUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final PhotoView photoView;

        ViewHolder(@NonNull PhotoView itemView) {
            super(itemView);
            this.photoView = itemView;
        }
    }
}
