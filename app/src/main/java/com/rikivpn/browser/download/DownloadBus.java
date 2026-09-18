package com.rikivpn.browser.download;

import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadBus {

    public interface Listener {
        void onUpdate(long systemDownloadId, String status, long downloaded, long total);
    }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void register(Listener l) { listeners.add(l); }
    public static void unregister(Listener l) { listeners.remove(l); }

    public static void publish(long id, String status, long downloaded, long total) {
        for (Listener l : listeners) l.onUpdate(id, status, downloaded, total);
    }
}
