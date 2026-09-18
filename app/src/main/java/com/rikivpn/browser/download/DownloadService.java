package com.rikivpn.browser.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.rikivpn.browser.BrowserActivity;
import com.rikivpn.browser.R;
import com.rikivpn.browser.data.AppDatabase;
import com.rikivpn.browser.data.Models;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Background download queue: jobs are processed one at a time (queueing several large files at
 * once is common on mobile data) while each individual file still downloads with the
 * multi-thread {@link DownloadEngine}. Runs as a foreground service so downloads survive the
 * browser activity being closed.
 */
public class DownloadService extends Service {

    public static final String ACTION_ENQUEUE = "com.rikivpn.browser.action.ENQUEUE";
    public static final String ACTION_PAUSE = "com.rikivpn.browser.action.PAUSE";
    public static final String ACTION_RESUME = "com.rikivpn.browser.action.RESUME";
    public static final String ACTION_CANCEL = "com.rikivpn.browser.action.CANCEL";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String EXTRA_ROW_ID = "row_id";

    private static final String CHANNEL_ID = "riki_downloads";
    private static final int NOTIF_ID = 5001;

    private final ConcurrentLinkedQueue<Models.DownloadItem> queue = new ConcurrentLinkedQueue<>();
    private final Map<Long, DownloadEngine> activeEngines = new LinkedHashMap<>();
    private boolean processing = false;
    private AppDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = AppDatabase.get(this);
        createChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_ENQUEUE.equals(action)) {
            String url = intent.getStringExtra(EXTRA_URL);
            String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
            enqueue(url, fileName);
        } else if (ACTION_PAUSE.equals(action)) {
            long rowId = intent.getLongExtra(EXTRA_ROW_ID, -1);
            DownloadEngine e = activeEngines.get(rowId);
            if (e != null) e.pause();
        } else if (ACTION_RESUME.equals(action)) {
            long rowId = intent.getLongExtra(EXTRA_ROW_ID, -1);
            DownloadEngine e = activeEngines.get(rowId);
            if (e != null) e.resume();
        } else if (ACTION_CANCEL.equals(action)) {
            long rowId = intent.getLongExtra(EXTRA_ROW_ID, -1);
            DownloadEngine e = activeEngines.get(rowId);
            if (e != null) { e.cancel(); activeEngines.remove(rowId); }
            db.deleteDownload(rowId);
        }
        return START_STICKY;
    }

    private void enqueue(String url, String fileName) {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        File dest = new File(dir, fileName);

        Models.DownloadItem item = new Models.DownloadItem();
        item.systemDownloadId = System.currentTimeMillis(); // stable local id for this engine
        item.fileName = fileName;
        item.url = url;
        item.localUri = dest.getAbsolutePath();
        item.status = "QUEUED";
        item.totalBytes = 0;
        item.downloadedBytes = 0;
        long rowId = db.addDownload(item);
        item.id = rowId;

        queue.add(item);
        startForeground(NOTIF_ID, buildNotification("Riki Downloads", "Preparing " + fileName + "…", 0, true));
        processNext();
    }

    private void processNext() {
        if (processing) return;
        Models.DownloadItem next = queue.poll();
        if (next == null) {
            stopForeground(true);
            return;
        }
        processing = true;
        db.updateDownloadProgress(next.systemDownloadId, "RUNNING", next.totalBytes, next.downloadedBytes, next.localUri);
        DownloadBus.publish(next.systemDownloadId, "RUNNING", 0, 0);

        DownloadEngine engine = new DownloadEngine(next.url, next.localUri, new DownloadEngine.Listener() {
            @Override
            public void onProgress(long downloaded, long total) {
                db.updateDownloadProgress(next.systemDownloadId, "RUNNING", total, downloaded, null);
                DownloadBus.publish(next.systemDownloadId, "RUNNING", downloaded, total);
                int pct = total > 0 ? (int) (downloaded * 100 / total) : 0;
                startForeground(NOTIF_ID, buildNotification("Downloading " + next.fileName, pct + "%", pct, true));
            }

            @Override
            public void onComplete(String localPath) {
                db.updateDownloadProgress(next.systemDownloadId, "COMPLETE", next.totalBytes, next.totalBytes, localPath);
                DownloadBus.publish(next.systemDownloadId, "COMPLETE", next.totalBytes, next.totalBytes);
                activeEngines.remove(next.id);
                processing = false;
                processNext();
            }

            @Override
            public void onError(String message) {
                db.updateDownloadProgress(next.systemDownloadId, "FAILED", next.totalBytes, next.downloadedBytes, null);
                DownloadBus.publish(next.systemDownloadId, "FAILED", next.downloadedBytes, next.totalBytes);
                activeEngines.remove(next.id);
                processing = false;
                processNext();
            }

            @Override
            public void onPaused() {
                db.updateDownloadProgress(next.systemDownloadId, "PAUSED", next.totalBytes, next.downloadedBytes, null);
                DownloadBus.publish(next.systemDownloadId, "PAUSED", next.downloadedBytes, next.totalBytes);
                processing = false;
                processNext();
            }
        });

        activeEngines.put(next.id, engine);
        engine.start();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Downloads",
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String title, String text, int progress, boolean ongoing) {
        Intent openIntent = new Intent(this, BrowserActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_download)
                .setOngoing(ongoing)
                .setProgress(100, progress, progress <= 0)
                .setContentIntent(pi)
                .build();
    }
}
