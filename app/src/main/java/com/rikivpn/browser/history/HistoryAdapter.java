package com.rikivpn.browser.history;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.R;
import com.rikivpn.browser.data.Models;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface Listener {
        void onOpen(Models.HistoryEntry entry);
        void onDelete(Models.HistoryEntry entry);
    }

    private final List<Models.HistoryEntry> items;
    private final Listener listener;

    public HistoryAdapter(List<Models.HistoryEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.HistoryEntry h = items.get(position);
        holder.title.setText(h.title == null || h.title.isEmpty() ? h.url : h.title);
        holder.url.setText(h.url);
        holder.time.setText(DateFormat.format("dd MMM, hh:mm a", h.visitedAt));
        holder.itemView.setOnClickListener(v -> listener.onOpen(h));
        holder.delete.setOnClickListener(v -> listener.onDelete(h));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, url, time;
        ImageButton delete;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvHistoryTitle);
            url = itemView.findViewById(R.id.tvHistoryUrl);
            time = itemView.findViewById(R.id.tvHistoryTime);
            delete = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}
