package com.rikivpn.browser;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.rikivpn.browser.bookmarks.BookmarksActivity;
import com.rikivpn.browser.browser.BrowserChromeClient;
import com.rikivpn.browser.browser.BrowserWebViewClient;
import com.rikivpn.browser.browser.Tab;
import com.rikivpn.browser.browser.TabsAdapter;
import com.rikivpn.browser.core.AppPreferences;
import com.rikivpn.browser.core.ReaderModeHelper;
import com.rikivpn.browser.core.SearchEngine;
import com.rikivpn.browser.data.AppDatabase;
import com.rikivpn.browser.data.KeystoreCrypto;
import com.rikivpn.browser.data.Models;
import com.rikivpn.browser.download.DownloadService;
import com.rikivpn.browser.download.DownloadsActivity;
import com.rikivpn.browser.history.HistoryActivity;
import com.rikivpn.browser.passwords.PasswordManagerActivity;
import com.rikivpn.browser.settings.SettingsActivity;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class BrowserActivity extends AppCompatActivity {

    public static final String HOME_URL = "https://www.google.com";

    private FrameLayout webViewContainer;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etUrl;
    private ImageButton btnTabs, btnMenu, btnBack, btnForward, btnHome, btnVpn;
    private ImageButton btnBottomVpn, btnBottomSettings;
    private TextView tvTabCount;
    private View findBar;
    private EditText etFind;
    private TextView tvFindCount;
    private RecyclerView rvTabs;

    private final List<Tab> tabs = new ArrayList<>();
    private int currentIndex = -1;
    private TabsAdapter tabsAdapter;
    private AppDatabase db;

    private final ActivityResultLauncher<Intent> voiceLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        navigateOrSearch(matches.get(0));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.rikivpn.browser.core.ThemeHelper.applyNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        com.rikivpn.browser.core.ThemeHelper.applyAmoledOverrideIfNeeded(this);

        db = AppDatabase.get(this);
        bindViews();
        setupTabsStrip();
        setupUrlBar();
        setupBottomBar();
        setupFindBar();

        String openUrl = getIntent().getStringExtra(BookmarksActivity.EXTRA_OPEN_URL);
        openNewTab(openUrl != null ? openUrl : HOME_URL, false);

        refreshVpnIndicator();

        if (AppPreferences.isVpnAutoConnect(this)) {
            startActivity(new Intent(this, VpnActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshVpnIndicator();
    }

    private void bindViews() {
        webViewContainer = findViewById(R.id.webViewContainer);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        etUrl = findViewById(R.id.etUrl);
        btnTabs = findViewById(R.id.btnTabs);
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnHome = findViewById(R.id.btnHome);
        btnVpn = findViewById(R.id.btnVpn);
        btnBottomVpn = findViewById(R.id.btnBottomVpn);
        btnBottomSettings = findViewById(R.id.btnBottomSettings);
        tvTabCount = findViewById(R.id.tvTabCount);
        findBar = findViewById(R.id.findBar);
        etFind = findViewById(R.id.etFind);
        tvFindCount = findViewById(R.id.tvFindCount);
        rvTabs = findViewById(R.id.rvTabs);

        swipeRefresh.setOnRefreshListener(() -> {
            if (currentTab() != null) currentTab().webView.reload();
        });

        View.OnClickListener openVpn = v -> startActivity(new Intent(this, VpnActivity.class));
        btnVpn.setOnClickListener(openVpn);
        btnBottomVpn.setOnClickListener(openVpn);
        btnBottomSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // Top-bar overflow (three-dot) button: shows the full browser menu as a popup,
        // anchored to the button itself so it always appears regardless of action-bar state.
        btnMenu.setOnClickListener(this::showOverflowMenu);
    }

    /** Refreshes the VPN shield icon tint (top bar + bottom nav) to reflect connection state. */
    private void refreshVpnIndicator() {
        int color = androidx.core.content.ContextCompat.getColor(this,
                com.rikivpn.browser.core.VpnState.isConnected() ? R.color.riki_success : R.color.riki_text_secondary);
        btnVpn.setColorFilter(color);
        btnBottomVpn.setColorFilter(color);
    }

    private void showOverflowMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_browser, popup.getMenu());
        MenuItem bookmarkItem = popup.getMenu().findItem(R.id.action_bookmark_page);
        if (currentTab() != null && db.isBookmarked(currentTab().url)) {
            bookmarkItem.setTitle(R.string.remove_bookmark);
        } else {
            bookmarkItem.setTitle(R.string.add_bookmark);
        }
        popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
        popup.show();
    }

    private void setupTabsStrip() {
        rvTabs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tabsAdapter = new TabsAdapter(tabs, new TabsAdapter.Listener() {
            @Override
            public void onTabSelected(int position) {
                switchToTab(position);
                rvTabs.setVisibility(View.GONE);
            }

            @Override
            public void onTabClosed(int position) {
                closeTab(position);
            }
        });
        rvTabs.setAdapter(tabsAdapter);
        btnTabs.setOnClickListener(v ->
                rvTabs.setVisibility(rvTabs.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    }

    private void setupUrlBar() {
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                navigateOrSearch(etUrl.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupBottomBar() {
        btnBack.setOnClickListener(v -> {
            if (currentTab() != null && currentTab().webView.canGoBack()) currentTab().webView.goBack();
        });
        btnForward.setOnClickListener(v -> {
            if (currentTab() != null && currentTab().webView.canGoForward()) currentTab().webView.goForward();
        });
        btnHome.setOnClickListener(v -> navigateOrSearch(HOME_URL));
    }

    private void setupFindBar() {
        etFind.setOnEditorActionListener((v, actionId, event) -> {
            if (currentTab() != null) {
                currentTab().webView.findAllAsync(etFind.getText().toString());
            }
            return true;
        });
        findViewById(R.id.btnFindNext).setOnClickListener(v -> {
            if (currentTab() != null) currentTab().webView.findNext(true);
        });
        findViewById(R.id.btnFindPrev).setOnClickListener(v -> {
            if (currentTab() != null) currentTab().webView.findNext(false);
        });
        findViewById(R.id.btnFindClose).setOnClickListener(v -> {
            findBar.setVisibility(View.GONE);
            if (currentTab() != null) currentTab().webView.clearMatches();
        });
    }

    // ------------------------------------------------------------------
    // Tabs
    // ------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private WebView createWebView(boolean incognito) {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(!AppPreferences.isDataSaverEnabled(this));
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (incognito) {
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setSaveFormData(false);
            CookieManager.getInstance().setAcceptCookie(true);
        }

        if (AppPreferences.isDesktopModeDefault(this)) {
            applyDesktopUserAgent(webView);
        }

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
            Intent intent = new Intent(BrowserActivity.this, DownloadService.class);
            intent.setAction(DownloadService.ACTION_ENQUEUE);
            intent.putExtra(DownloadService.EXTRA_URL, url);
            intent.putExtra(DownloadService.EXTRA_FILE_NAME, fileName);
            startService(intent);
            Toast.makeText(BrowserActivity.this, getString(R.string.download_started, fileName), Toast.LENGTH_SHORT).show();
        });

        return webView;
    }

    private Tab openNewTab(String url, boolean incognito) {
        WebView webView = createWebView(incognito);
        Tab tab = new Tab(webView, incognito);

        webView.setWebViewClient(new BrowserWebViewClient(this, tab, new BrowserWebViewClient.Callback() {
            @Override
            public void onTitleOrProgress(WebView view) {
                if (currentTab() != null && currentTab().webView == view) {
                    etUrl.setText(tab.url);
                    swipeRefresh.setRefreshing(tab.progress > 0 && tab.progress < 100);
                }
                tabsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onPageFinishedLoading(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
                maybePrefillPassword(view, url);
            }
        }));

        webView.setWebChromeClient(new BrowserChromeClient(this, tab, new BrowserChromeClient.Callback() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (currentTab() != null && currentTab().webView == view) {
                    swipeRefresh.setRefreshing(newProgress < 100);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) { tabsAdapter.notifyDataSetChanged(); }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) { tabsAdapter.notifyDataSetChanged(); }

            @Override
            public WebView onNewTabRequested(boolean parentIncognito) {
                Tab newTab = openNewTab("", parentIncognito);
                return newTab.webView;
            }
        }));

        tabs.add(tab);
        webViewContainer.addView(webView);
        switchToTab(tabs.size() - 1);

        if (url != null && !url.isEmpty()) webView.loadUrl(url);
        tabsAdapter.notifyDataSetChanged();
        return tab;
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).webView.setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
        currentIndex = index;
        Tab tab = tabs.get(index);
        etUrl.setText(tab.url);
        tvTabCount.setText(String.valueOf(tabs.size()));
        tabsAdapter.setSelected(index);
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        Tab tab = tabs.remove(index);
        webViewContainer.removeView(tab.webView);
        tab.webView.destroy();
        if (tabs.isEmpty()) {
            openNewTab(HOME_URL, false);
        } else {
            switchToTab(Math.min(index, tabs.size() - 1));
        }
        tabsAdapter.notifyDataSetChanged();
    }

    private Tab currentTab() {
        return (currentIndex >= 0 && currentIndex < tabs.size()) ? tabs.get(currentIndex) : null;
    }

    // ------------------------------------------------------------------
    // Navigation / search
    // ------------------------------------------------------------------

    private void navigateOrSearch(String input) {
        if (currentTab() == null) { openNewTab(HOME_URL, false); return; }
        String url;
        if (Patterns.WEB_URL.matcher(input).matches() || input.startsWith("http://") || input.startsWith("https://")) {
            url = input.startsWith("http") ? input : "https://" + input;
        } else {
            SearchEngine engine = SearchEngine.fromPrefsValue(AppPreferences.getSearchEngine(this));
            url = engine.buildSearchUrl(input);
        }
        currentTab().webView.loadUrl(url);
        rvTabs.setVisibility(View.GONE);
    }

    private void applyDesktopUserAgent(WebView webView) {
        String desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        webView.getSettings().setUserAgentString(desktopUA);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
    }

    private void toggleDesktopMode() {
        Tab tab = currentTab();
        if (tab == null) return;
        WebSettings settings = tab.webView.getSettings();
        boolean isDesktopNow = settings.getUserAgentString() != null
                && settings.getUserAgentString().contains("Windows NT");
        if (isDesktopNow) {
            settings.setUserAgentString(null); // reset to system default mobile UA
        } else {
            applyDesktopUserAgent(tab.webView);
        }
        tab.webView.reload();
    }

    private void maybePrefillPassword(WebView view, String url) {
        if (currentTab() == null || currentTab().webView != view || url == null) return;
        Uri uri = Uri.parse(url);
        List<Models.PasswordEntry> matches = db.findPasswordsForSite(uri.getHost());
        if (!matches.isEmpty()) {
            Toast.makeText(this, getString(R.string.saved_login_found, uri.getHost()), Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem bookmarkItem = menu.findItem(R.id.action_bookmark_page);
        if (currentTab() != null && db.isBookmarked(currentTab().url)) {
            bookmarkItem.setTitle(R.string.remove_bookmark);
        } else {
            bookmarkItem.setTitle(R.string.add_bookmark);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_tab) {
            openNewTab(HOME_URL, false);
        } else if (id == R.id.action_new_incognito_tab) {
            openNewTab(HOME_URL, true);
        } else if (id == R.id.action_desktop_mode) {
            toggleDesktopMode();
        } else if (id == R.id.action_find_in_page) {
            findBar.setVisibility(View.VISIBLE);
            etFind.requestFocus();
        } else if (id == R.id.action_reader_mode) {
            if (currentTab() != null) currentTab().webView.evaluateJavascript(ReaderModeHelper.TOGGLE_JS, null);
        } else if (id == R.id.action_voice_search) {
            launchVoiceSearch();
        } else if (id == R.id.action_qr_scan) {
            new IntentIntegrator(this).initiateScan();
        } else if (id == R.id.action_bookmark_page) {
            toggleBookmarkCurrentPage();
        } else if (id == R.id.action_bookmarks) {
            startActivity(new Intent(this, BookmarksActivity.class));
        } else if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        } else if (id == R.id.action_downloads) {
            startActivity(new Intent(this, DownloadsActivity.class));
        } else if (id == R.id.action_passwords) {
            startActivity(new Intent(this, PasswordManagerActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_share) {
            shareCurrentPage();
        } else if (id == R.id.action_add_shortcut) {
            addHomeScreenShortcut();
        } else if (id == R.id.action_vpn) {
            startActivity(new Intent(this, VpnActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_search_prompt));
        try {
            voiceLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.voice_search_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleBookmarkCurrentPage() {
        Tab tab = currentTab();
        if (tab == null || tab.url == null || tab.url.isEmpty()) return;
        if (db.isBookmarked(tab.url)) {
            db.removeBookmark(tab.url);
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
        } else {
            db.addBookmark(tab.title, tab.url);
            Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentPage() {
        if (currentTab() == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, currentTab().url);
        startActivity(Intent.createChooser(share, getString(R.string.share_via)));
    }

    private void addHomeScreenShortcut() {
        if (currentTab() == null || currentTab().url == null) return;
        Uri uri = Uri.parse(currentTab().url);
        String host = uri.getHost() != null ? uri.getHost() : currentTab().url;

        androidx.core.content.pm.ShortcutInfoCompat shortcut =
                new androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "shortcut_" + System.currentTimeMillis())
                        .setShortLabel(host)
                        .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                        .setIntent(new Intent(Intent.ACTION_VIEW, uri, this, BrowserActivity.class)
                                .putExtra(BookmarksActivity.EXTRA_OPEN_URL, currentTab().url))
                        .build();
        androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null && scanResult.getContents() != null) {
            navigateOrSearch(scanResult.getContents());
        }
    }

    @Override
    public void onBackPressed() {
        Tab tab = currentTab();
        if (findBar.getVisibility() == View.VISIBLE) {
            findBar.setVisibility(View.GONE);
            if (tab != null) tab.webView.clearMatches();
        } else if (rvTabs.getVisibility() == View.VISIBLE) {
            rvTabs.setVisibility(View.GONE);
        } else if (tab != null && tab.webView.canGoBack()) {
            tab.webView.goBack();
        } else if (tabs.size() > 1) {
            closeTab(currentIndex);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Tab tab : tabs) tab.webView.destroy();
    }
}
