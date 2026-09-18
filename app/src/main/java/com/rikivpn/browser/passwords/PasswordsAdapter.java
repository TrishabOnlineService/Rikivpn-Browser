package com.rikivpn.browser.passwords;

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

public class PasswordsAdapter extends RecyclerView.Adapter<PasswordsAdapter.VH> {

    public interface Listener {
        void onReveal(Models.PasswordEntry entry, TextView target);
        void onDelete(Models.PasswordEntry entry);
    }

    private final List<Models.PasswordEntry> items;
    private final Listener listener;

    public PasswordsAdapter(List<Models.PasswordEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_password, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.PasswordEntry p = items.get(position);
        holder.site.setText(p.site);
        holder.username.setText(p.username);
        holder.password.setText("••••••••");
        holder.reveal.setOnClickListener(v -> listener.onReveal(p, holder.password));
        holder.delete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView site, username, password;
        ImageButton reveal, delete;

        VH(@NonNull View itemView) {
            super(itemView);
            site = itemView.findViewById(R.id.tvPwSite);
            username = itemView.findViewById(R.id.tvPwUsername);
            password = itemView.findViewById(R.id.tvPwPassword);
            reveal = itemView.findViewById(R.id.btnRevealPassword);
            delete = itemView.findViewById(R.id.btnDeletePassword);
        }
    }
}
