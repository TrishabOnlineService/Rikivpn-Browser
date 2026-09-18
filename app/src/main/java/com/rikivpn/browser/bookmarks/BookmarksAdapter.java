package com.rikivpn.browser.bookmarks;

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

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.VH> {

    public interface Listener {
        void onOpen(Models.Bookmark bookmark);
        void onDelete(Models.Bookmark bookmark);
    }

    private final List<Models.Bookmark> items;
    private final Listener listener;

    public BookmarksAdapter(List<Models.Bookmark> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Bookmark b = items.get(position);
        holder.title.setText(b.title == null || b.title.isEmpty() ? b.url : b.title);
        holder.url.setText(b.url);
        holder.itemView.setOnClickListener(v -> listener.onOpen(b));
        holder.delete.setOnClickListener(v -> listener.onDelete(b));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, url;
        ImageButton delete;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvBookmarkTitle);
            url = itemView.findViewById(R.id.tvBookmarkUrl);
            delete = itemView.findViewById(R.id.btnDeleteBookmark);
        }
    }
}
