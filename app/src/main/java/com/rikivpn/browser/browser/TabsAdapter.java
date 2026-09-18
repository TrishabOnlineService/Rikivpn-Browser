package com.rikivpn.browser.browser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.R;

import java.util.List;

public class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.VH> {

    public interface Listener {
        void onTabSelected(int position);
        void onTabClosed(int position);
    }

    private final List<Tab> tabs;
    private final Listener listener;
    private int selected;

    public TabsAdapter(List<Tab> tabs, Listener listener) {
        this.tabs = tabs;
        this.listener = listener;
    }

    public void setSelected(int position) {
        selected = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Tab tab = tabs.get(position);
        holder.title.setText(tab.title == null || tab.title.isEmpty() ? "New Tab" : tab.title);
        holder.itemView.setSelected(position == selected);
        holder.incognitoBadge.setVisibility(tab.incognito ? View.VISIBLE : View.GONE);
        if (tab.favicon != null) {
            holder.icon.setImageBitmap(tab.favicon);
        } else {
            holder.icon.setImageResource(R.drawable.ic_tab_placeholder);
        }
        holder.itemView.setOnClickListener(v -> listener.onTabSelected(holder.getAdapterPosition()));
        holder.close.setOnClickListener(v -> listener.onTabClosed(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon, close, incognitoBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTabTitle);
            icon = itemView.findViewById(R.id.ivTabIcon);
            close = itemView.findViewById(R.id.ivTabClose);
            incognitoBadge = itemView.findViewById(R.id.ivIncognitoBadge);
        }
    }
}
