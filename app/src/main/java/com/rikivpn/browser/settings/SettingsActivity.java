package com.rikivpn.browser.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.rikivpn.browser.R;
import com.rikivpn.browser.core.AppPreferences;
import com.rikivpn.browser.core.LocaleHelper;
import com.rikivpn.browser.data.AppDatabase;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings_title);
        findViewById(R.id.btnSettingsBack).setOnClickListener(v -> finish());

        setupThemeRow();
        setupSwitch(R.id.swAdBlock, AppPreferences.isAdBlockEnabled(this), AppPreferences::setAdBlockEnabled);
        setupSwitch(R.id.swTrackerBlock, AppPreferences.isTrackerBlockEnabled(this), AppPreferences::setTrackerBlockEnabled);
        setupSwitch(R.id.swPopupBlock, AppPreferences.isPopupBlockEnabled(this), AppPreferences::setPopupBlockEnabled);
        setupSwitch(R.id.swHttpsOnly, AppPreferences.isHttpsOnly(this), AppPreferences::setHttpsOnly);
        setupSwitch(R.id.swSafeBrowsing, AppPreferences.isSafeBrowsingEnabled(this), AppPreferences::setSafeBrowsingEnabled);
        setupSwitch(R.id.swDataSaver, AppPreferences.isDataSaverEnabled(this), AppPreferences::setDataSaverEnabled);
        setupSwitch(R.id.swSearchSuggestions, AppPreferences.isSearchSuggestionsEnabled(this), AppPreferences::setSearchSuggestionsEnabled);
        setupSwitch(R.id.swVpnAutoConnect, AppPreferences.isVpnAutoConnect(this), AppPreferences::setVpnAutoConnect);
        setupSwitch(R.id.swVpnAutoReconnect, AppPreferences.isAutoReconnect(this), AppPreferences::setAutoReconnect);
        setupSwitch(R.id.swVpnKillSwitch, AppPreferences.isKillSwitchEnabled(this), AppPreferences::setKillSwitchEnabled);
        setupSwitch(R.id.swVpnDnsProtection, AppPreferences.isDnsProtectionEnabled(this), AppPreferences::setDnsProtectionEnabled);
        setupSwitch(R.id.swVpnAutoFastest, AppPreferences.isAutoFastestServer(this), AppPreferences::setAutoFastestServer);

        setupSearchEngineSpinner();
        setupProtocolRow();
        setupLanguageSpinner();

        findViewById(R.id.rowClearData).setOnClickListener(v -> confirmClearData());
    }

    private interface Setter { void set(android.content.Context c, boolean v); }

    private void setupSwitch(int id, boolean current, Setter setter) {
        SwitchMaterial sw = findViewById(id);
        sw.setChecked(current);
        sw.setOnCheckedChangeListener((btn, checked) -> setter.set(this, checked));
    }

    private void setupThemeRow() {
        RadioGroup group = findViewById(R.id.radioGroupTheme);
        int theme = AppPreferences.getTheme(this);
        int checkedId = theme == AppPreferences.THEME_LIGHT ? R.id.radioLight
                : theme == AppPreferences.THEME_AMOLED ? R.id.radioAmoled : R.id.radioDark;
        group.check(checkedId);
        group.setOnCheckedChangeListener((g, checkedIdNow) -> {
            int newTheme = checkedIdNow == R.id.radioLight ? AppPreferences.THEME_LIGHT
                    : checkedIdNow == R.id.radioAmoled ? AppPreferences.THEME_AMOLED : AppPreferences.THEME_DARK;
            AppPreferences.setTheme(this, newTheme);
            com.rikivpn.browser.core.ThemeHelper.applyNightMode(this);
            recreate();
        });
    }

    private void setupSearchEngineSpinner() {
        Spinner spinner = findViewById(R.id.spinnerSearchEngine);
        spinner.setSelection(AppPreferences.getSearchEngine(this));
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                AppPreferences.setSearchEngine(SettingsActivity.this, position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void setupProtocolRow() {
        RadioGroup group = findViewById(R.id.radioGroupProtocol);
        boolean isTcp = "TCP".equals(AppPreferences.getVpnProtocol(this));
        group.check(isTcp ? R.id.radioTcp : R.id.radioUdp);
        group.setOnCheckedChangeListener((g, checkedId) ->
                AppPreferences.setVpnProtocol(this, checkedId == R.id.radioTcp ? "TCP" : "UDP"));
    }

    private void setupLanguageSpinner() {
        Spinner spinner = findViewById(R.id.spinnerLanguage);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, LocaleHelper.SUPPORTED_LABELS);
        spinner.setAdapter(adapter);
        String current = AppPreferences.getLanguageTag(this);
        for (int i = 0; i < LocaleHelper.SUPPORTED_TAGS.length; i++) {
            if (LocaleHelper.SUPPORTED_TAGS[i].equals(current)) spinner.setSelection(i);
        }
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String tag = LocaleHelper.SUPPORTED_TAGS[position];
                if (!tag.equals(AppPreferences.getLanguageTag(SettingsActivity.this))) {
                    AppPreferences.setLanguageTag(SettingsActivity.this, tag);
                    LocaleHelper.apply(tag);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_message)
                .setPositiveButton(R.string.clear, (d, w) -> clearAllBrowsingData())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void clearAllBrowsingData() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        AppDatabase.get(this).clearHistory();
        try {
            deleteDatabase("riki_browser_cache");
        } catch (Exception ignored) { }
        Toast.makeText(this, R.string.data_cleared, Toast.LENGTH_SHORT).show();
    }
}
