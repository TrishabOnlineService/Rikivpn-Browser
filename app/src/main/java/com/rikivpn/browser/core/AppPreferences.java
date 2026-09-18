package com.rikivpn.browser.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single SharedPreferences facade for every toggle in Settings so no activity
 * touches raw SharedPreferences keys directly.
 */
public class AppPreferences {

    private static final String PREFS = "riki_browser_prefs";

    // Theme
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_AMOLED = 2;

    // Search engines
    public static final int ENGINE_GOOGLE = 0;
    public static final int ENGINE_BING = 1;
    public static final int ENGINE_DUCKDUCKGO = 2;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // --- Browser / privacy ---
    public static boolean isAdBlockEnabled(Context c) { return prefs(c).getBoolean("ad_block", true); }
    public static void setAdBlockEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("ad_block", v).apply(); }

    public static boolean isPopupBlockEnabled(Context c) { return prefs(c).getBoolean("popup_block", true); }
    public static void setPopupBlockEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("popup_block", v).apply(); }

    public static boolean isTrackerBlockEnabled(Context c) { return prefs(c).getBoolean("tracker_block", true); }
    public static void setTrackerBlockEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("tracker_block", v).apply(); }

    public static boolean isHttpsOnly(Context c) { return prefs(c).getBoolean("https_only", false); }
    public static void setHttpsOnly(Context c, boolean v) { prefs(c).edit().putBoolean("https_only", v).apply(); }

    public static boolean isSafeBrowsingEnabled(Context c) { return prefs(c).getBoolean("safe_browsing", true); }
    public static void setSafeBrowsingEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("safe_browsing", v).apply(); }

    public static boolean isDataSaverEnabled(Context c) { return prefs(c).getBoolean("data_saver", false); }
    public static void setDataSaverEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("data_saver", v).apply(); }

    public static boolean isDesktopModeDefault(Context c) { return prefs(c).getBoolean("desktop_default", false); }
    public static void setDesktopModeDefault(Context c, boolean v) { prefs(c).edit().putBoolean("desktop_default", v).apply(); }

    public static int getSearchEngine(Context c) { return prefs(c).getInt("search_engine", ENGINE_GOOGLE); }
    public static void setSearchEngine(Context c, int engine) { prefs(c).edit().putInt("search_engine", engine).apply(); }

    public static boolean isSearchSuggestionsEnabled(Context c) { return prefs(c).getBoolean("search_suggestions", true); }
    public static void setSearchSuggestionsEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("search_suggestions", v).apply(); }

    // --- Theme / language ---
    public static int getTheme(Context c) { return prefs(c).getInt("theme_mode", THEME_DARK); }
    public static void setTheme(Context c, int theme) { prefs(c).edit().putInt("theme_mode", theme).apply(); }

    public static String getLanguageTag(Context c) { return prefs(c).getString("language_tag", "en"); }
    public static void setLanguageTag(Context c, String tag) { prefs(c).edit().putString("language_tag", tag).apply(); }

    // --- VPN ---
    public static boolean isVpnAutoConnect(Context c) { return prefs(c).getBoolean("vpn_auto_connect", false); }
    public static void setVpnAutoConnect(Context c, boolean v) { prefs(c).edit().putBoolean("vpn_auto_connect", v).apply(); }

    public static boolean isAutoReconnect(Context c) { return prefs(c).getBoolean("vpn_auto_reconnect", true); }
    public static void setAutoReconnect(Context c, boolean v) { prefs(c).edit().putBoolean("vpn_auto_reconnect", v).apply(); }

    public static boolean isKillSwitchEnabled(Context c) { return prefs(c).getBoolean("vpn_kill_switch", false); }
    public static void setKillSwitchEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("vpn_kill_switch", v).apply(); }

    public static boolean isDnsProtectionEnabled(Context c) { return prefs(c).getBoolean("vpn_dns_protection", true); }
    public static void setDnsProtectionEnabled(Context c, boolean v) { prefs(c).edit().putBoolean("vpn_dns_protection", v).apply(); }

    public static boolean isAutoFastestServer(Context c) { return prefs(c).getBoolean("vpn_auto_fastest", false); }
    public static void setAutoFastestServer(Context c, boolean v) { prefs(c).edit().putBoolean("vpn_auto_fastest", v).apply(); }

    public static String getVpnProtocol(Context c) { return prefs(c).getString("vpn_protocol", "UDP"); }
    public static void setVpnProtocol(Context c, String protocol) { prefs(c).edit().putString("vpn_protocol", protocol).apply(); }

    // --- One-time reader onboarding for split tunneling notice, etc. ---
    public static boolean getBool(Context c, String key, boolean def) { return prefs(c).getBoolean(key, def); }
    public static void setBool(Context c, String key, boolean v) { prefs(c).edit().putBoolean(key, v).apply(); }
}
