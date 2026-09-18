package com.rikivpn.browser.core;

import android.net.Uri;

public enum SearchEngine {
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com/complete/search?client=firefox&q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com/osjson.aspx?query=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/html/?q=%s", "https://duckduckgo.com/ac/?q=%s&type=list");

    public final String label;
    public final String searchUrlTemplate;
    public final String suggestUrlTemplate;

    SearchEngine(String label, String searchUrlTemplate, String suggestUrlTemplate) {
        this.label = label;
        this.searchUrlTemplate = searchUrlTemplate;
        this.suggestUrlTemplate = suggestUrlTemplate;
    }

    public String buildSearchUrl(String query) {
        return String.format(searchUrlTemplate, Uri.encode(query));
    }

    public String buildSuggestUrl(String query) {
        return String.format(suggestUrlTemplate, Uri.encode(query));
    }

    public static SearchEngine fromPrefsValue(int value) {
        switch (value) {
            case AppPreferences.ENGINE_BING: return BING;
            case AppPreferences.ENGINE_DUCKDUCKGO: return DUCKDUCKGO;
            default: return GOOGLE;
        }
    }
}
