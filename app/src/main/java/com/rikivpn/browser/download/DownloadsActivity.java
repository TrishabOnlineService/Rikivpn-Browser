package com.rikivpn.browser.download;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.R;
import com.rikivpn.browser.data.AppDatabase;
import com.rikivpn.browser.data.Models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity implements DownloadBus.Listener {

    private final List<Models.DownloadItem> items = new ArrayList<>();
    private DownloadsAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);
        setTitle(R.string.downloads_title);

        db = AppDatabase.get(this);
        RecyclerView rv = findViewById(R.id.rvDownloads);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadsAdapter(items, new DownloadsAdapter.Listener() {
            @Override
            public void onPause(Models.DownloadItem item) { sendAction(DownloadService.ACTION_PAUSE, item); }

            @Override
            public void onResume(Models.DownloadItem item) { sendAction(DownloadService.ACTION_RESUME, item); }

            @Override
            public void onCancel(Models.DownloadItem item) {
                sendAction(DownloadService.ACTION_CANCEL, item);
                items.remove(item);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onOpen(Models.DownloadItem item) { openFile(item); }
        });
        rv.setAdapter(adapter);

        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(db.getDownloads());
        adapter.notifyDataSetChanged();
    }

    private void sendAction(String action, Models.DownloadItem item) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(action);
        intent.putExtra(DownloadService.EXTRA_ROW_ID, item.id);
        startService(intent);
    }

    private void openFile(Models.DownloadItem item) {
        if (item.localUri == null) return;
        try {
            File file = new File(item.localUri);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownloadBus.register(this);
        reload();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DownloadBus.unregister(this);
    }

    @Override
    public void onUpdate(long systemDownloadId, String status, long downloaded, long total) {
        runOnUiThread(this::reload);
    }
}
