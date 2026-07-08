package com.unifurniture.mobile.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.PointTransactionDto;

import java.util.ArrayList;
import java.util.List;

public class PointHistoryAdapter extends RecyclerView.Adapter<PointHistoryAdapter.ViewHolder> {

    private final List<PointTransactionDto> list = new ArrayList<>();

    public PointHistoryAdapter() {
    }

    public void setData(List<PointTransactionDto> data) {
        this.list.clear();
        if (data != null) {
            this.list.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_point_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PointTransactionDto item = list.get(position);
        Context context = holder.itemView.getContext();

        // 1. Title and points style based on transaction type
        String title;
        boolean isSubtraction = false;

        switch (item.getType()) {
            case "earn":
                title = "Nhận điểm từ đơn hàng";
                break;
            case "review_earn":
                title = "Thưởng đánh giá sản phẩm";
                break;
            case "redeem":
                title = "Sử dụng điểm giảm giá";
                isSubtraction = true;
                break;
            case "expire":
                title = "Điểm hết hạn";
                isSubtraction = true;
                break;
            case "manual_adjust":
                title = "Điều chỉnh bởi cửa hàng";
                break;
            default:
                title = "Giao dịch tích điểm";
                break;
        }

        holder.tvTitle.setText(title);
        holder.tvNote.setText(item.getNote() != null ? item.getNote() : "");
        holder.tvTime.setText(formatDateTime(item.getCreatedAt()));

        // 2. Display points with positive/negative prefix and color
        if (isSubtraction) {
            holder.tvPoints.setText(String.format("-%d", item.getPoints()));
            holder.tvPoints.setTextColor(ContextCompat.getColor(context, R.color.discount_red));
        } else {
            holder.tvPoints.setText(String.format("+%d", item.getPoints()));
            // Use primary brand color or green for earned points
            holder.tvPoints.setTextColor(ContextCompat.getColor(context, R.color.primary));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String formatDateTime(String isoString) {
        if (isoString == null || isoString.length() < 10) return "";
        try {
            String datePart = isoString.substring(0, 10); // "YYYY-MM-DD"
            String[] ymd = datePart.split("-");
            String formattedDate = ymd[2] + "/" + ymd[1] + "/" + ymd[0];
            if (isoString.length() >= 16) {
                String timePart = isoString.substring(11, 16); // "HH:MM"
                return timePart + " " + formattedDate;
            }
            return formattedDate;
        } catch (Exception e) {
            return isoString;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvNote;
        TextView tvTime;
        TextView tvPoints;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivTransactionIcon);
            tvTitle = itemView.findViewById(R.id.tvTransactionTitle);
            tvNote = itemView.findViewById(R.id.tvTransactionNote);
            tvTime = itemView.findViewById(R.id.tvTransactionTime);
            tvPoints = itemView.findViewById(R.id.tvTransactionPoints);
        }
    }
}
