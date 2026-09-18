package com.rikivpn.browser.browser;

import android.net.Uri;
import android.os.Build;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import com.rikivpn.browser.core.AdBlockManager;
import com.rikivpn.browser.core.AppPreferences;
import com.rikivpn.browser.core.VpnState;
import com.rikivpn.browser.data.AppDatabase;

public class BrowserWebViewClient extends WebViewClient {

    public interface Callback {
        void onTitleOrProgress(WebView view);
        void onPageFinishedLoading(WebView view, String url);
    }

    private final android.content.Context context;
    private final Tab tab;
    private final Callback callback;

    public BrowserWebViewClient(android.content.Context context, Tab tab, Callback callback) {
        this.context = context;
        this.tab = tab;
        this.callback = callback;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String scheme = uri.getScheme();

        // Kill Switch: while enabled, no page navigation is allowed unless BongoVpn reports
        // a connected tunnel. This is enforced at the app/WebView layer (there is no OS-level
        // firewall hook exposed by the bundled VPN engine), so it stops the browser itself
        // from leaking traffic outside the tunnel, but does not block other apps on the device.
        if (AppPreferences.isKillSwitchEnabled(context) && !VpnState.isConnected()) {
            view.loadUrl("about:blank");
            return true;
        }

        // Hand off non-http(s) schemes (tel:, mailto:, intent:, market:) to the system.
        if (scheme != null && !scheme.equals("http") && !scheme.equals("https")) {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);
                context.startActivity(intent);
            } catch (Exception ignored) { /* no app can handle it, just stay put */ }
            return true;
        }

        // HTTPS Only Mode: silently upgrade http:// navigations.
        if ("http".equals(scheme) && AppPreferences.isHttpsOnly(context)) {
            Uri upgraded = uri.buildUpon().scheme("https").build();
            view.loadUrl(upgraded.toString());
            return true;
        }

        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (AppPreferences.isKillSwitchEnabled(context) && !VpnState.isConnected()) {
            return AdBlockManager.emptyResponse();
        }
        String host = request.getUrl().getHost();
        boolean adBlockOn = AppPreferences.isAdBlockEnabled(context) || AppPreferences.isTrackerBlockEnabled(context);
        if (adBlockOn && AdBlockManager.get(context).isBlocked(host)) {
            return AdBlockManager.emptyResponse();
        }
        return super.shouldInterceptRequest(view, request);
    }

    @RequiresApi(api = Build.VERSION_CODES.O_MR1)
    @Override
    public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
        if (AppPreferences.isSafeBrowsingEnabled(context)) {
            // Show the interstitial WebView already renders and report back so the user can
            // still choose "proceed anyway" from that built-in screen.
            callback.showInterstitial(true);
        } else {
            callback.proceed(true);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        tab.url = url;
        if (callback != null) callback.onTitleOrProgress(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        tab.title = view.getTitle() != null ? view.getTitle() : url;
        tab.url = url;
        if (!tab.incognito && url != null && !url.startsWith("about:") && !url.startsWith("data:")) {
            AppDatabase.get(context).addHistory(tab.title, url);
        }
        if (callback != null) {
            callback.onTitleOrProgress(view);
            callback.onPageFinishedLoading(view, url);
        }
    }
}
