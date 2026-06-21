package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ProductDto;
import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.VH> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(ProductDto product);
    }

    private final List<ProductDto> items = new ArrayList<>();
    private final OnSuggestionClickListener listener;

    public SearchSuggestionAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProductDto> products) {
        items.clear();
        items.addAll(products);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new VH(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProductDto product = items.get(position);
        holder.tv.setText(product.name);
        holder.tv.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick(product);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        VH(TextView tv) { super(tv); this.tv = tv; }
    }
}
