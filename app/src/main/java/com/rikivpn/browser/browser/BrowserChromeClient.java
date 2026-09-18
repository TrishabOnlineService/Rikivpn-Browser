package com.rikivpn.browser.browser;

import android.graphics.Bitmap;
import android.os.Message;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.rikivpn.browser.core.AppPreferences;

public class BrowserChromeClient extends WebChromeClient {

    public interface Callback {
        void onProgressChanged(WebView view, int newProgress);
        void onReceivedTitle(WebView view, String title);
        void onReceivedIcon(WebView view, Bitmap icon);
        /** Return the WebView of the newly created tab, or null if the popup was blocked. */
        WebView onNewTabRequested(boolean incognito);
    }

    private final android.content.Context context;
    private final Tab tab;
    private final Callback callback;

    public BrowserChromeClient(android.content.Context context, Tab tab, Callback callback) {
        this.context = context;
        this.tab = tab;
        this.callback = callback;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        tab.progress = newProgress;
        if (callback != null) callback.onProgressChanged(view, newProgress);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        tab.title = title;
        if (callback != null) callback.onReceivedTitle(view, title);
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        tab.favicon = icon;
        if (callback != null) callback.onReceivedIcon(view, icon);
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        boolean popupBlockOn = AppPreferences.isPopupBlockEnabled(context);
        if (popupBlockOn && !isUserGesture) {
            // Classic ad-popup pattern: JS calling window.open() with no tap behind it.
            return false;
        }
        if (callback == null) return false;
        WebView newWebView = callback.onNewTabRequested(tab.incognito);
        if (newWebView == null) return false;

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        return true;
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        // Deny camera/mic/etc. by default; the browser's own voice-search and QR features use
        // native Android intents (RecognizerIntent / a dedicated scanner) instead of in-page
        // getUserMedia, so no site gets silent access to the microphone or camera.
        request.deny();
    }

    @Override
    public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
        return true; // swallow console spam instead of flooding logcat
    }
}
