package com.rikivpn.browser.browser;

import android.graphics.Bitmap;
import android.webkit.WebView;

public class Tab {
    public WebView webView;
    public String title = "New Tab";
    public String url = "";
    public Bitmap favicon;
    public boolean incognito;
    public int progress = 0;

    public Tab(WebView webView, boolean incognito) {
        this.webView = webView;
        this.incognito = incognito;
    }
}
