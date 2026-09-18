package com.rikivpn.browser.core;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Sets the app's UI language via the AndroidX per-app language API, which is backed by the
 * platform API on Android 13+ and by AppCompat's own locale-override mechanism on older
 * versions (down to API 26, our minSdk) — so Language Selection works app-wide without needing
 * a manifest locale config bump per release.
 */
public class LocaleHelper {

    public static final String[] SUPPORTED_TAGS = {"en", "bn", "hi"};
    public static final String[] SUPPORTED_LABELS = {"English", "বাংলা (Bengali)", "हिन्दी (Hindi)"};

    public static void apply(String languageTag) {
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(locales);
    }
}
