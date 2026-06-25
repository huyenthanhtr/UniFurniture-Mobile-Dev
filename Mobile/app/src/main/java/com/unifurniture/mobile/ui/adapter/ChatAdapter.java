package com.unifurniture.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.ui.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

/** Renders chat bubbles: user messages on the right, assistant on the left. */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<ChatMessage> items = new ArrayList<>();
    private Markwon markwon;

    private Markwon markwon(@NonNull View view) {
        if (markwon == null) markwon = Markwon.create(view.getContext());
        return markwon;
    }

    public void submit(List<ChatMessage> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).from; // FROM_USER / FROM_BOT
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ChatMessage.FROM_USER
                ? R.layout.item_chat_user
                : R.layout.item_chat_bot;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        if (msg.from == ChatMessage.FROM_BOT) {
            if (msg.pending && msg.retrying && msg.text.isEmpty()) {
                // Backend is retrying a transient model error.
                holder.text.setText(holder.itemView.getContext()
                        .getString(R.string.chat_retrying, msg.retryAttempt, msg.retryMax));
            } else if (msg.pending && msg.text.isEmpty()) {
                // Still waiting for the first chunk — show a typing placeholder.
                holder.text.setText("…");
            } else if (msg.pending) {
                // Streaming: render plain text (avoids re-parsing partial markdown on every chunk).
                holder.text.setText(msg.text);
            } else {
                // Finished: render the full reply as markdown (bold, lists, links…).
                markwon(holder.itemView).setMarkdown(holder.text, msg.text);
            }
        } else {
            holder.text.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tvMessage);
        }
    }
}
