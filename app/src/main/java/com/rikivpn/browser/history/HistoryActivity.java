package com.rikivpn.browser.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.BrowserActivity;
import com.rikivpn.browser.R;
import com.rikivpn.browser.bookmarks.BookmarksActivity;
import com.rikivpn.browser.data.AppDatabase;
import com.rikivpn.browser.data.Models;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private final List<Models.HistoryEntry> items = new ArrayList<>();
    private HistoryAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setTitle(R.string.history_title);

        db = AppDatabase.get(this);
        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(items, new HistoryAdapter.Listener() {
            @Override
            public void onOpen(Models.HistoryEntry entry) {
                Intent intent = new Intent(HistoryActivity.this, BrowserActivity.class);
                intent.putExtra(BookmarksActivity.EXTRA_OPEN_URL, entry.url);
                startActivity(intent);
                finish();
            }

            @Override
            public void onDelete(Models.HistoryEntry entry) {
                db.deleteHistoryEntry(entry.id);
                reload();
            }
        });
        rv.setAdapter(adapter);
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(db.getHistory());
        adapter.notifyDataSetChanged();
        findViewById(R.id.tvEmptyHistory).setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_history) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_history_title)
                    .setMessage(R.string.clear_history_message)
                    .setPositiveButton(R.string.clear, (d, w) -> { db.clearHistory(); reload(); })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
