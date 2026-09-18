package com.rikivpn.browser.download;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.R;
import com.rikivpn.browser.data.Models;

import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.VH> {

    public interface Listener {
        void onPause(Models.DownloadItem item);
        void onResume(Models.DownloadItem item);
        void onCancel(Models.DownloadItem item);
        void onOpen(Models.DownloadItem item);
    }

    private final List<Models.DownloadItem> items;
    private final Listener listener;

    public DownloadsAdapter(List<Models.DownloadItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.DownloadItem item = items.get(position);
        holder.name.setText(item.fileName);
        holder.status.setText(item.status);
        int pct = item.totalBytes > 0 ? (int) (item.downloadedBytes * 100 / item.totalBytes) : 0;
        holder.progress.setProgress(pct);

        boolean running = "RUNNING".equals(item.status) || "QUEUED".equals(item.status);
        boolean paused = "PAUSED".equals(item.status);
        boolean complete = "COMPLETE".equals(item.status);

        holder.btnPause.setVisibility(running ? View.VISIBLE : View.GONE);
        holder.btnResume.setVisibility(paused ? View.VISIBLE : View.GONE);
        holder.btnCancel.setVisibility(complete ? View.GONE : View.VISIBLE);
        holder.btnOpen.setVisibility(complete ? View.VISIBLE : View.GONE);

        holder.btnPause.setOnClickListener(v -> listener.onPause(item));
        holder.btnResume.setOnClickListener(v -> listener.onResume(item));
        holder.btnCancel.setOnClickListener(v -> listener.onCancel(item));
        holder.btnOpen.setOnClickListener(v -> listener.onOpen(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, status;
        ProgressBar progress;
        ImageButton btnPause, btnResume, btnCancel, btnOpen;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvDownloadName);
            status = itemView.findViewById(R.id.tvDownloadStatus);
            progress = itemView.findViewById(R.id.progressDownload);
            btnPause = itemView.findViewById(R.id.btnPause);
            btnResume = itemView.findViewById(R.id.btnResume);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnOpen = itemView.findViewById(R.id.btnOpen);
        }
    }
}
