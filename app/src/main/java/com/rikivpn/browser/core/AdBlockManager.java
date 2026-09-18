package com.rikivpn.browser.core;

import android.content.Context;
import android.webkit.WebResourceResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight, host-name based ad/tracker blocker.
 *
 * This is a starter block-list (assets/adblock_hosts.txt), not a full EasyList/EasyPrivacy
 * mirror — it covers the most common ad-serving and tracking domains and blocks any request
 * whose host matches or is a sub-domain of an entry. Requests to blocked hosts get an empty
 * 200 response instead of being fetched, which is enough to stop most banner/interstitial ads
 * and tracking pixels without breaking page layout the way a hard connection error would.
 */
public class AdBlockManager {

    private static volatile AdBlockManager instance;
    private final Set<String> blockedHosts = new HashSet<>();
    private volatile boolean loaded = false;

    public static AdBlockManager get(Context context) {
        if (instance == null) {
            synchronized (AdBlockManager.class) {
                if (instance == null) instance = new AdBlockManager();
            }
        }
        instance.ensureLoaded(context.getApplicationContext());
        return instance;
    }

    private void ensureLoaded(Context context) {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            try (InputStream is = context.getAssets().open("adblock_hosts.txt");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    blockedHosts.add(line.toLowerCase());
                }
            } catch (IOException e) {
                // No list bundled -> ad block simply has no effect, never crash the browser for it.
            }
            loaded = true;
        }
    }

    public boolean isBlocked(String host) {
        if (host == null) return false;
        host = host.toLowerCase();
        if (blockedHosts.contains(host)) return true;
        int dot = host.indexOf('.');
        while (dot != -1) {
            host = host.substring(dot + 1);
            if (blockedHosts.contains(host)) return true;
            dot = host.indexOf('.');
        }
        return false;
    }

    /** An empty, harmless response used to swallow a blocked request. */
    public static WebResourceResponse emptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream(new byte[0]));
    }

    public Set<String> snapshot() {
        return Collections.unmodifiableSet(blockedHosts);
    }
}
