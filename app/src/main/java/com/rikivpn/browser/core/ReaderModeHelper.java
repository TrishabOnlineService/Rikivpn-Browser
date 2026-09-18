package com.rikivpn.browser.core;

/**
 * Reader Mode is implemented as a JS injection rather than a server-side readability service:
 * it hides nav/aside/script/ad elements and re-renders the largest text block in a clean,
 * high-contrast column. It is a heuristic, not a full Readability.js port, so results vary by
 * site — but it needs no network calls or extra permissions and works fully offline/on-VPN.
 */
public class ReaderModeHelper {

    public static final String TOGGLE_JS =
            "(function(){" +
            "  if (window.__rikiReaderActive) {" +
            "    document.getElementById('riki-reader-overlay') && document.getElementById('riki-reader-overlay').remove();" +
            "    document.body.style.overflow = '';" +
            "    window.__rikiReaderActive = false;" +
            "    return;" +
            "  }" +
            "  var candidates = document.querySelectorAll('article, main, [role=main], .post, .article, .entry-content, #content');" +
            "  var best = null, bestLen = 0;" +
            "  var pool = candidates.length ? candidates : document.querySelectorAll('div, section');" +
            "  for (var i = 0; i < pool.length; i++) {" +
            "    var len = (pool[i].innerText || '').length;" +
            "    if (len > bestLen) { bestLen = len; best = pool[i]; }" +
            "  }" +
            "  if (!best || bestLen < 200) { best = document.body; }" +
            "  var overlay = document.createElement('div');" +
            "  overlay.id = 'riki-reader-overlay';" +
            "  overlay.style.cssText = 'position:fixed;inset:0;z-index:2147483647;background:#F5F0E6;color:#1a1a1a;" +
            "overflow:auto;padding:24px;font-family:Georgia,serif;font-size:19px;line-height:1.6;'" +
            ";" +
            "  var title = document.createElement('h1');" +
            "  title.style.cssText = 'font-size:26px;margin-bottom:16px;';" +
            "  title.innerText = document.title;" +
            "  overlay.appendChild(title);" +
            "  var content = document.createElement('div');" +
            "  content.innerHTML = best.innerHTML;" +
            "  var kill = content.querySelectorAll('script, style, iframe, nav, aside, form, button, .ad, .ads, .advert');" +
            "  for (var k = 0; k < kill.length; k++) kill[k].remove();" +
            "  overlay.appendChild(content);" +
            "  document.body.appendChild(overlay);" +
            "  document.body.style.overflow = 'hidden';" +
            "  window.__rikiReaderActive = true;" +
            "})();";
}
