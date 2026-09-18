package com.rikivpn.browser.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "riki_browser.db";
    private static final int DB_VERSION = 1;

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get(Context context) {
        if (instance == null) {
            instance = new AppDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private AppDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE bookmarks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT UNIQUE, added_at INTEGER)");
        db.execSQL("CREATE TABLE history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT, visited_at INTEGER)");
        db.execSQL("CREATE TABLE downloads (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, system_download_id INTEGER, file_name TEXT, " +
                "url TEXT, local_uri TEXT, status TEXT, total_bytes INTEGER, downloaded_bytes INTEGER, created_at INTEGER)");
        db.execSQL("CREATE TABLE passwords (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, site TEXT, username TEXT, " +
                "encrypted_password TEXT, updated_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS bookmarks");
        db.execSQL("DROP TABLE IF EXISTS history");
        db.execSQL("DROP TABLE IF EXISTS downloads");
        db.execSQL("DROP TABLE IF EXISTS passwords");
        onCreate(db);
    }

    // ---------------- Bookmarks ----------------

    public boolean isBookmarked(String url) {
        try (Cursor c = getReadableDatabase().query("bookmarks", new String[]{"id"}, "url=?",
                new String[]{url}, null, null, null)) {
            return c.moveToFirst();
        }
    }

    public void addBookmark(String title, String url) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("url", url);
        cv.put("added_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("bookmarks", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeBookmark(String url) {
        getWritableDatabase().delete("bookmarks", "url=?", new String[]{url});
    }

    public List<Models.Bookmark> getBookmarks() {
        List<Models.Bookmark> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("bookmarks", null, null, null, null, null, "added_at DESC")) {
            while (c.moveToNext()) {
                Models.Bookmark b = new Models.Bookmark();
                b.id = c.getLong(c.getColumnIndexOrThrow("id"));
                b.title = c.getString(c.getColumnIndexOrThrow("title"));
                b.url = c.getString(c.getColumnIndexOrThrow("url"));
                b.addedAt = c.getLong(c.getColumnIndexOrThrow("added_at"));
                list.add(b);
            }
        }
        return list;
    }

    public void deleteBookmark(long id) {
        getWritableDatabase().delete("bookmarks", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------- History ----------------

    public void addHistory(String title, String url) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("url", url);
        cv.put("visited_at", System.currentTimeMillis());
        getWritableDatabase().insert("history", null, cv);
    }

    public List<Models.HistoryEntry> getHistory() {
        List<Models.HistoryEntry> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("history", null, null, null, null, null, "visited_at DESC", "500")) {
            while (c.moveToNext()) {
                Models.HistoryEntry h = new Models.HistoryEntry();
                h.id = c.getLong(c.getColumnIndexOrThrow("id"));
                h.title = c.getString(c.getColumnIndexOrThrow("title"));
                h.url = c.getString(c.getColumnIndexOrThrow("url"));
                h.visitedAt = c.getLong(c.getColumnIndexOrThrow("visited_at"));
                list.add(h);
            }
        }
        return list;
    }

    public void deleteHistoryEntry(long id) {
        getWritableDatabase().delete("history", "id=?", new String[]{String.valueOf(id)});
    }

    public void clearHistory() {
        getWritableDatabase().delete("history", null, null);
    }

    // ---------------- Downloads ----------------

    public long addDownload(Models.DownloadItem item) {
        ContentValues cv = new ContentValues();
        cv.put("system_download_id", item.systemDownloadId);
        cv.put("file_name", item.fileName);
        cv.put("url", item.url);
        cv.put("local_uri", item.localUri);
        cv.put("status", item.status);
        cv.put("total_bytes", item.totalBytes);
        cv.put("downloaded_bytes", item.downloadedBytes);
        cv.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("downloads", null, cv);
    }

    public void updateDownloadProgress(long systemDownloadId, String status, long total, long downloaded, String localUri) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("total_bytes", total);
        cv.put("downloaded_bytes", downloaded);
        if (localUri != null) cv.put("local_uri", localUri);
        getWritableDatabase().update("downloads", cv, "system_download_id=?",
                new String[]{String.valueOf(systemDownloadId)});
    }

    public List<Models.DownloadItem> getDownloads() {
        List<Models.DownloadItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("downloads", null, null, null, null, null, "created_at DESC")) {
            while (c.moveToNext()) {
                Models.DownloadItem d = new Models.DownloadItem();
                d.id = c.getLong(c.getColumnIndexOrThrow("id"));
                d.systemDownloadId = c.getLong(c.getColumnIndexOrThrow("system_download_id"));
                d.fileName = c.getString(c.getColumnIndexOrThrow("file_name"));
                d.url = c.getString(c.getColumnIndexOrThrow("url"));
                d.localUri = c.getString(c.getColumnIndexOrThrow("local_uri"));
                d.status = c.getString(c.getColumnIndexOrThrow("status"));
                d.totalBytes = c.getLong(c.getColumnIndexOrThrow("total_bytes"));
                d.downloadedBytes = c.getLong(c.getColumnIndexOrThrow("downloaded_bytes"));
                d.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
                list.add(d);
            }
        }
        return list;
    }

    public void deleteDownload(long id) {
        getWritableDatabase().delete("downloads", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------- Passwords ----------------

    public long savePassword(Models.PasswordEntry entry) {
        ContentValues cv = new ContentValues();
        cv.put("site", entry.site);
        cv.put("username", entry.username);
        cv.put("encrypted_password", entry.encryptedPassword);
        cv.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insert("passwords", null, cv);
    }

    public List<Models.PasswordEntry> getPasswords() {
        List<Models.PasswordEntry> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("passwords", null, null, null, null, null, "site ASC")) {
            while (c.moveToNext()) {
                Models.PasswordEntry p = new Models.PasswordEntry();
                p.id = c.getLong(c.getColumnIndexOrThrow("id"));
                p.site = c.getString(c.getColumnIndexOrThrow("site"));
                p.username = c.getString(c.getColumnIndexOrThrow("username"));
                p.encryptedPassword = c.getString(c.getColumnIndexOrThrow("encrypted_password"));
                p.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
                list.add(p);
            }
        }
        return list;
    }

    public List<Models.PasswordEntry> findPasswordsForSite(String host) {
        List<Models.PasswordEntry> all = getPasswords();
        List<Models.PasswordEntry> matches = new ArrayList<>();
        for (Models.PasswordEntry p : all) {
            if (host != null && p.site != null && host.contains(p.site)) matches.add(p);
        }
        return matches;
    }

    public void deletePassword(long id) {
        getWritableDatabase().delete("passwords", "id=?", new String[]{String.valueOf(id)});
    }
}
