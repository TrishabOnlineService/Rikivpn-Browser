package com.rikivpn.browser.bookmarks;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.BrowserActivity;
import com.rikivpn.browser.R;
import com.rikivpn.browser.data.AppDatabase;
import com.rikivpn.browser.data.Models;

import java.util.ArrayList;
import java.util.List;

public class BookmarksActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_URL = "open_url";

    private final List<Models.Bookmark> items = new ArrayList<>();
    private BookmarksAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        setTitle(R.string.bookmarks_title);

        db = AppDatabase.get(this);
        RecyclerView rv = findViewById(R.id.rvBookmarks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookmarksAdapter(items, new BookmarksAdapter.Listener() {
            @Override
            public void onOpen(Models.Bookmark bookmark) {
                Intent intent = new Intent(BookmarksActivity.this, BrowserActivity.class);
                intent.putExtra(EXTRA_OPEN_URL, bookmark.url);
                startActivity(intent);
                finish();
            }

            @Override
            public void onDelete(Models.Bookmark bookmark) {
                db.deleteBookmark(bookmark.id);
                reload();
            }
        });
        rv.setAdapter(adapter);
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(db.getBookmarks());
        adapter.notifyDataSetChanged();
        findViewById(R.id.tvEmptyBookmarks).setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
