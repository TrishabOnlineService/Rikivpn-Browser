package com.rikivpn.browser.core;

import android.app.Activity;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    /** Call from Application.onCreate or the first activity, before setContentView. */
    public static void applyNightMode(android.content.Context context) {
        int mode = AppPreferences.getTheme(context);
        if (mode == AppPreferences.THEME_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // Dark and AMOLED-Dark both use the dark theme resources; AMOLED additionally
            // swaps surface colors to pure black via applyAmoledOverride() below.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /** For AMOLED mode, force key surfaces to pure black. Call after setContentView. */
    public static void applyAmoledOverrideIfNeeded(Activity activity) {
        if (AppPreferences.getTheme(activity) != AppPreferences.THEME_AMOLED) return;
        android.view.View root = activity.findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(0xFF000000);
        }
        activity.getWindow().setStatusBarColor(0xFF000000);
        activity.getWindow().setNavigationBarColor(0xFF000000);
    }
}
