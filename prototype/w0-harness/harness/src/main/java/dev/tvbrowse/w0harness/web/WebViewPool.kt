package dev.tvbrowse.w0harness.web

import android.annotation.SuppressLint
import android.app.Activity
import android.os.BadParcelableException
import android.os.Bundle
import android.os.Parcel
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import dev.tvbrowse.w0harness.BuildConfig
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Imperative WebView host (brief §3 / AA §1.2) and the M2 tab pool (§5.3).
 *
 * The Compose tree sees exactly one plain [FrameLayout] (via a single
 * AndroidView). WebViews are created, attached, detached and destroyed here —
 * never inside a composable factory.
 *
 * M2: at most 2 live WebViews (active + standby per the T-05 standby mode),
 * up to 10 tabs whose stored state is the app-owned URL stack ([TabManager]).
 * Stored tabs are rebuilt on switch; the evicted live instance follows the
 * AA §6.3 destroy sequence. Renderer exits are recovered from stored state
 * under a crash-loop guard (§5.4). Fullscreen views go to a host-provided
 * container OUTSIDE the Compose tree (AA D-gap-09). A local asset origin
 * (appassets.androidplatform.net) serves the bundled M2 test pages — no
 * network (brief §1.5).
 */
class WebViewPool(private val activity: Activity, private val settings: SettingsStore) {

    /** The single container exposed to Compose. Outlives screen navigation. */
    val hostContainer: FrameLayout = FrameLayout(activity)

    /** Fullscreen (custom view) host, implemented by the activity (§5.2 M2 part). */
    interface FullscreenHost {
        /** Attaches the custom view to the full-window overlay container. */
        fun enterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback)

        /** Hides the overlay when it is showing; returns whether it was. */
        fun exitFullscreen(): Boolean
    }

    var fullscreenHost: FullscreenHost? = null

    val manager: TabManager = TabManager().apply {
        standbyMode = StandbyMode.fromId(settings.standbyMode()) ?: StandbyMode.PREWARM
    }

    private val persistence = TabPersistence(activity)
    private val crashGuard = CrashLoopGuard(windowMs = settings.crashGuardWindowSeconds() * 1000L)

    /** tabId -> live WebView. Insertion-order map; only the active one is attached. */
    private val live = LinkedHashMap<Int, WebView>()

    /** In-memory saveState Bundles of evicted tabs (brief §5.3 "if needed"). */
    private val savedBundles = HashMap<Int, Bundle>()

    /** Maps live instances to their tab id (renderer callbacks only see the view). */
    private val viewToTab = HashMap<WebView, Int>()

    /** Blank prewarm WebView (standby mode a); not attached, not a tab. */
    private var prewarm: WebView? = null

    /** Switch-latency measurement (§5.3): request -> first visual state / page finished. */
    private var switchStartedElapsedMs: Long? = null
    private var switchPendingTabId: Int? = null
    private var switchRestoredVia: String? = null

    private val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(activity))
        .build()

    val activeWebView: WebView?
        get() = manager.activeTabId?.let { live[it] }

    val currentWebView: WebView?
        get() = activeWebView

    // ---------------------------------------------------------------- creation

    /**
     * Makes sure the active tab has a live WebView and is attached (T-17 guard
     * kept from M1). Returns false when the WebView provider is unavailable.
     */
    fun ensureCreated(): Boolean {
        AppState.webViewError = null
        if (manager.activeTab() == null) manager.openTab(null)
        val tabId = manager.activeTabId ?: return false
        val created = rebuildTab(tabId, reason = "ensure_created") != null
        ensurePrewarm()
        updateHudCounts()
        return created
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tabId: Int): WebView? {
        val provider = runCatching { WebViewCompat.getCurrentWebViewPackage(activity) }.getOrNull()
        if (provider == null) {
            SessionLog.log("WEBVIEW_PROVIDER_NULL", jsonObject("hint" to "getCurrentWebViewPackage returned null"))
        }
        val start = SystemClock.uptimeMillis()
        val created: WebView = try {
            WebView(activity)
        } catch (t: Throwable) {
            val msg = "WebView unavailable: ${t.javaClass.simpleName}: ${t.message ?: ""}"
            AppState.webViewError = msg
            SessionLog.log(
                "WEBVIEW_UNAVAILABLE",
                jsonObject(
                    "errorClass" to t.javaClass.name,
                    "message" to (t.message ?: ""),
                    "providerPackage" to provider?.packageName,
                ),
            )
            return null
        }
        val durationMs = SystemClock.uptimeMillis() - start
        val isFirst = AppState.firstWebViewConstructorMs == null
        if (isFirst) AppState.firstWebViewConstructorMs = durationMs
        configure(created)
        live[tabId] = created
        viewToTab[created] = tabId
        SessionLog.log(
            "WEBVIEW_CREATED",
            jsonObject(
                "tabId" to tabId,
                "constructorDurationMs" to durationMs,
                "isFirstConstructor" to isFirst,
                "providerPackage" to provider?.packageName,
                "providerVersion" to provider?.versionName,
                "liveInstances" to live.size,
            ),
        )
        return created
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(wv: WebView) {
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        if (settings.desktopUserAgent()) {
            wv.settings.userAgentString = DESKTOP_USER_AGENT
        }
        applySafeBrowsing(wv)
        wv.webViewClient = HarnessWebViewClient()
        wv.webChromeClient = HarnessChromeClient()
    }

    /** Blank, detached, warm WebView for standby mode (a); never attached. */
    private fun ensurePrewarm() {
        if (manager.standbyMode != StandbyMode.PREWARM || prewarm != null) return
        if (live.size >= TabManager.MAX_LIVE) return
        val start = SystemClock.uptimeMillis()
        val wv = runCatching {
            @SuppressLint("SetJavaScriptEnabled")
            WebView(activity).apply {
                settings.javaScriptEnabled = true
                webViewClient = HarnessWebViewClient()
            }
        }.getOrNull() ?: return
        prewarm = wv
        SessionLog.log(
            "WEBVIEW_PREWARM_CREATED",
            jsonObject(
                "constructorDurationMs" to (SystemClock.uptimeMillis() - start),
                "standbyMode" to manager.standbyMode.id,
                "liveInstancesIncludingPrewarm" to (live.size + 1),
            ),
        )
        updateHudCounts()
    }

    private fun destroyPrewarm(reason: String) {
        prewarm?.let { wv ->
            SessionLog.log("WEBVIEW_PREWARM_DESTROY", jsonObject("reason" to reason))
            runCatching { wv.destroy() }
        }
        prewarm = null
        updateHudCounts()
    }

    /**
     * Per-WebView Safe Browsing (§3.1): default off, runtime toggle. Always logs
     * state before and after; in the `alt` flavor the readback shows whether
     * runtime enabling took effect at all (manifest opt-out, T-15).
     */
    fun applySafeBrowsing(wv: WebView) {
        val requested = settings.safeBrowsingEnabled()
        val featureSupported = WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)
        val before = runCatching { WebSettingsCompat.getSafeBrowsingEnabled(wv.settings) }.getOrNull()
        var readback: Boolean? = null
        if (featureSupported) {
            runCatching {
                WebSettingsCompat.setSafeBrowsingEnabled(wv.settings, requested)
                readback = WebSettingsCompat.getSafeBrowsingEnabled(wv.settings)
            }
        }
        SessionLog.log(
            "SAFE_BROWSING_APPLIED",
            jsonObject(
                "requested" to requested,
                "before" to before,
                "afterReadback" to readback,
                "featureSupported" to featureSupported,
                "runtimeEnableEffective" to (if (requested) readback else null),
                "flavor" to BuildConfig.FLAVOR,
            ),
        )
    }

    // ---------------------------------------------------------------- attach / evict / destroy

    private fun attach(wv: WebView) {
        if (wv.parent == null) {
            hostContainer.addView(
                wv,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
            )
        }
        wv.requestFocus()
    }

    private fun detach(wv: WebView) {
        (wv.parent as? ViewGroup)?.removeView(wv)
    }

    /**
     * Records scroll + (optionally) a saveState Bundle, then destroys a live
     * instance with the AA §6.3 sequence: remove from parent -> stopLoading ->
     * saveState -> clearHistory -> destroy.
     */
    private fun destroyTabInstance(tabId: Int, reason: String, keepRestoreBundle: Boolean) {
        val wv = live.remove(tabId) ?: return
        viewToTab.remove(wv)
        manager.updateTab(tabId) { it.copy(scrollY = wv.scrollY) }
        detach(wv)
        wv.stopLoading()
        if (keepRestoreBundle) {
            runCatching {
                val bundle = Bundle()
                if (wv.saveState(bundle) != null) savedBundles[tabId] = bundle
            }.onFailure { t ->
                SessionLog.log(
                    "SAVE_STATE_FAILED",
                    jsonObject("tabId" to tabId, "errorClass" to t.javaClass.name, "message" to (t.message ?: "")),
                )
            }
            savedBundles[tabId]?.let { bundle ->
                SessionLog.log("SAVE_STATE_STORED", jsonObject("tabId" to tabId, "approxBytes" to bundleApproxBytes(bundle)))
            }
        }
        runCatching { wv.clearHistory() }
        runCatching { wv.destroy() }
        SessionLog.log(
            "WEBVIEW_DESTROY",
            jsonObject("tabId" to tabId, "reason" to reason, "liveInstances" to live.size),
        )
        updateHudCounts()
    }

    /**
     * Destroys every live instance (activity teardown, renderer recovery). With
     * [keepRestoreBundle] the instances are still usable for a later rebuild.
     */
    fun destroyAllLive(reason: String, keepRestoreBundle: Boolean = false) {
        for (tabId in live.keys.toList()) destroyTabInstance(tabId, reason, keepRestoreBundle)
        if (!keepRestoreBundle) destroyPrewarm(reason)
    }

    /** Kept for the activity teardown path (M1 name). */
    fun destroyAll(reason: String) {
        destroyAllLive(reason, keepRestoreBundle = false)
    }

    /** Enforces the live set: evicts live tabs the standby mode does not allow. */
    private fun evictToLiveSet(reason: String) {
        val allowed = manager.liveTabIds()
        for (tabId in manager.evictionCandidates(live.keys)) {
            if (tabId !in allowed) destroyTabInstance(tabId, reason, keepRestoreBundle = true)
        }
        if (manager.standbyMode == StandbyMode.PREWARM) ensurePrewarm() else destroyPrewarm(reason)
    }

    // ---------------------------------------------------------------- tabs (§5.3)

    /**
     * Creates/attaches the live WebView of [tabId]: reuse the live instance,
     * else restore a saveState Bundle, else load the stored URL. Returns the
     * live WebView or null when creation failed (T-17).
     */
    private fun rebuildTab(tabId: Int, reason: String): WebView? {
        val tab = manager.tab(tabId) ?: return null
        if (live.containsKey(tabId)) {
            val wv = live[tabId]!!
            attach(wv)
            SessionLog.log("TAB_ATTACHED", jsonObject("tabId" to tabId, "via" to "already_live", "reason" to reason))
            return wv
        }
        val wv = createWebView(tabId) ?: return null
        var restoredVia = "url_stack"
        val bundle = savedBundles.remove(tabId)
        if (bundle != null && runCatching { wv.restoreState(bundle) }.getOrNull() != null) {
            restoredVia = "savestate_bundle"
        }
        attach(wv)
        val target = tab.currentUrl
        if (restoredVia == "url_stack" && target != null) wv.loadUrl(target)
        SessionLog.log(
            "TAB_RESTORED",
            jsonObject(
                "tabId" to tabId,
                "via" to restoredVia,
                "url" to target,
                "scrollY" to tab.scrollY,
                "reason" to reason,
                "liveInstances" to live.size,
            ),
        )
        updateHudCounts()
        return wv
    }

    /** Opens a new tab (optionally with a first URL) and switches to it. */
    fun openTab(url: String?): TabState? {
        val tab = manager.openTab(url)
        if (tab == null) {
            SessionLog.log("TAB_OPEN_DENIED", jsonObject("reason" to "tab cap reached", "tabs" to manager.tabCount))
            return null
        }
        SessionLog.log("TAB_OPENED", jsonObject("tabId" to tab.id, "url" to url, "tabs" to manager.tabCount))
        evictToLiveSet("tab_opened")
        detachInactive()
        switchPendingTabId = tab.id
        switchRestoredVia = null
        switchStartedElapsedMs = SystemClock.elapsedRealtime()
        rebuildTab(tab.id, reason = "tab_opened")
        persistTabState("tab_opened")
        return tab
    }

    fun closeActiveTab() {
        val id = manager.activeTabId ?: return
        destroyTabInstance(id, reason = "tab_closed", keepRestoreBundle = false)
        savedBundles.remove(id)
        manager.closeTab(id)
        SessionLog.log("TAB_CLOSED", jsonObject("tabId" to id, "tabs" to manager.tabCount))
        manager.activeTabId?.let { rebuildTab(it, reason = "after_close") }
        persistTabState("tab_closed")
    }

    /**
     * Switches the active tab (P3/T-05): records the switch request, evicts per
     * the standby mode, rebuilds the target and logs the switch latency once
     * the first visual state callback or page finish arrives.
     */
    fun switchTo(tabId: Int, trigger: String): Boolean {
        if (!manager.switchTo(tabId)) return false
        SessionLog.log(
            "TAB_SWITCH",
            jsonObject("tabId" to tabId, "trigger" to trigger, "standbyMode" to manager.standbyMode.id, "tabs" to manager.tabCount),
        )
        detachInactive()
        evictToLiveSet("tab_switch")
        switchPendingTabId = tabId
        switchRestoredVia = null
        switchStartedElapsedMs = SystemClock.elapsedRealtime()
        val wv = rebuildTab(tabId, reason = "tab_switch") ?: return true
        if (switchRestoredVia == null) switchRestoredVia = "existing_instance"
        if (switchStartedElapsedMs == null) switchStartedElapsedMs = SystemClock.elapsedRealtime()
        // Live instance: the visual state callback completes the latency measurement.
        runCatching {
            wv.postVisualStateCallback(
                0,
                object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        completeSwitchLatency(tabId, via = "visual_state")
                        wv.post { wv.scrollTo(0, manager.tab(tabId)?.scrollY ?: 0) }
                    }
                },
            )
        }
        // Some pages never produce a new frame (already at rest): bounded fallback.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (switchPendingTabId == tabId) completeSwitchLatency(tabId, via = "timeout_fallback")
        }, 3000)
        persistTabState("tab_switch")
        return true
    }

    private fun detachInactive() {
        val activeId = manager.activeTabId
        for ((tabId, wv) in live) {
            if (tabId != activeId && wv.parent != null) {
                manager.updateTab(tabId) { it.copy(scrollY = wv.scrollY) }
                detach(wv)
                SessionLog.log("TAB_DETACHED_TO_STANDBY", jsonObject("tabId" to tabId))
            }
        }
    }

    private fun completeSwitchLatency(tabId: Int, via: String) {
        if (switchPendingTabId != tabId) return
        val started = switchStartedElapsedMs
        switchPendingTabId = null
        switchStartedElapsedMs = null
        SessionLog.log(
            "TAB_SWITCH_COMPLETE",
            jsonObject(
                "tabId" to tabId,
                "latencyMs" to (started?.let { SystemClock.elapsedRealtime() - it } ?: -1L),
                "completedBy" to via,
                "restoredVia" to (switchRestoredVia ?: "unknown"),
            ),
        )
        switchRestoredVia = null
    }

    private fun updateHudCounts() {
        AppState.tabCount = manager.tabCount
        AppState.liveWebViewCount = live.size + (if (prewarm != null) 1 else 0)
    }

    /** Applies a new standby mode (T-05); evicts/prewarms accordingly. */
    fun setStandbyMode(mode: StandbyMode, trigger: String) {
        manager.standbyMode = mode
        SessionLog.log("STANDBY_MODE_SET", jsonObject("mode" to mode.id, "trigger" to trigger))
        evictToLiveSet("standby_mode_$mode")
        updateHudCounts()
        persistTabState("standby_mode")
    }

    // ---------------------------------------------------------------- navigation

    fun loadUrl(url: String) {
        val wv = activeWebView ?: run {
            SessionLog.log("LOAD_URL_SKIPPED", jsonObject("url" to url, "reason" to "no live WebView"))
            return
        }
        SessionLog.log("LOAD_URL", jsonObject("url" to url))
        wv.loadUrl(url)
    }

    /** HUD Forward: app-owned stack first (§5.5), WebView history as transport. */
    fun goForward() {
        val tabId = manager.activeTabId ?: return
        val expected = manager.forward(tabId)?.currentUrl
        val wv = live[tabId] ?: return
        if (expected != null) {
            logStackNav(tabId, expected, wv)
            if (wv.canGoForward()) wv.goForward() else wv.loadUrl(expected)
        } else if (wv.canGoForward()) {
            SessionLog.log("WEB_GO_FORWARD", jsonObject("tabId" to tabId, "stack" to "exhausted"))
            wv.goForward()
        }
    }

    fun reload() {
        val wv = activeWebView ?: return
        SessionLog.log("WEB_RELOAD", jsonObject("url" to (AppState.currentUrl ?: "")))
        wv.reload()
    }

    /** HUD Back button: same app-owned walk as the Back key. */
    fun goBack() {
        handleBack(trigger = "hud_button")
    }

    /**
     * System/HUD back inside the browser. Walks the app-owned URL stack (§5.5)
     * and logs where the WebView's own history diverges from it. Returns true
     * when the WebView consumed the step.
     */
    fun handleBack(trigger: String = "back_key"): Boolean {
        val tabId = manager.activeTabId ?: return false
        val wv = live[tabId] ?: return false
        val expected = manager.back(tabId)?.currentUrl
        if (expected != null) {
            logStackNav(tabId, expected, wv)
            if (wv.canGoBack()) {
                wv.goBack()
            } else {
                // WebView history is empty (restored tab): load the stack target directly.
                wv.loadUrl(expected)
            }
            return true
        }
        // Stack exhausted: fall back to the WebView's own history if it still has entries
        // (e.g. redirects pushed web history the stack collapsed).
        if (wv.canGoBack()) {
            SessionLog.log("WEB_GO_BACK", jsonObject("tabId" to tabId, "trigger" to trigger, "stack" to "exhausted"))
            wv.goBack()
            return true
        }
        return false
    }

    private fun logStackNav(tabId: Int, expected: String, wv: WebView) {
        val webCurrent = runCatching { wv.copyBackForwardList().currentItem?.url }.getOrNull()
        SessionLog.log(
            "STACK_NAV",
            jsonObject(
                "tabId" to tabId,
                "expected" to expected,
                "webViewCurrent" to webCurrent,
                "diverges" to (webCurrent != null && webCurrent != expected),
                "webViewCanGoBack" to wv.canGoBack(),
            ),
        )
    }

    fun focusPage() {
        activeWebView?.requestFocus()
    }

    // ---------------------------------------------------------------- M2 triggers (§5.4)

    /** Trigger 1: load chrome://crash in the active tab (didCrash()==true path). */
    fun triggerCrashInActive() {
        val wv = activeWebView ?: run {
            SessionLog.log("CRASH_TRIGGER_SKIPPED", jsonObject("reason" to "no live WebView"))
            return
        }
        SessionLog.log("CRASH_TRIGGER", jsonObject("method" to "chrome_crash", "tabId" to manager.activeTabId))
        wv.loadUrl("chrome://crash")
    }

    /**
     * Trigger 2: WebViewRenderProcess.terminate() (didCrash()==false path),
     * guarded by WEB_VIEW_RENDERER_TERMINATE.
     */
    fun terminateActiveRenderer() {
        val wv = activeWebView ?: run {
            SessionLog.log("RENDERER_TERMINATE_SKIPPED", jsonObject("reason" to "no live WebView"))
            return
        }
        val supported = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE)
        val renderer = if (supported) runCatching { WebViewCompat.getWebViewRenderProcess(wv) }.getOrNull() else null
        if (renderer != null) {
            SessionLog.log("RENDERER_TERMINATE_REQUESTED", jsonObject("tabId" to manager.activeTabId))
            runCatching { renderer.terminate() }
        } else {
            SessionLog.log(
                "RENDERER_TERMINATE_UNAVAILABLE",
                jsonObject("featureSupported" to supported, "renderer" to null),
            )
        }
    }

    /** Resets the crash guard after the tester dismisses the tripped error page. */
    fun resetCrashGuard() {
        crashGuard.reset()
        AppState.crashGuardTripped = false
        AppState.crashGuardDetail = null
        SessionLog.log("CRASH_GUARD_RESET", jsonObject())
    }

    /**
     * Renderer-exit handling (§5.4): log didCrash()/rendererPriorityAtExit(),
     * destroy ALL affected instances, recreate active (+standby) from stored
     * tab state — unless the crash-loop guard trips, which shows the
     * D-pad-dismissible error page instead.
     */
    private fun onRenderProcessGoneInternal(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
        val tabId = viewToTab[view]
        SessionLog.log(
            "RENDER_PROCESS_GONE",
            jsonObject(
                "tabId" to tabId,
                "didCrash" to detail.didCrash(),
                "rendererPriorityAtExit" to detail.rendererPriorityAtExit(),
                "liveInstances" to live.size,
            ),
        )
        activity.runOnUiThread {
            // The renderer may have hosted more than one instance: drop them all.
            destroyAllLive(reason = "renderer_gone", keepRestoreBundle = false)
            savedBundles.clear()
            val decision = crashGuard.recordAndDecide()
            if (decision == CrashLoopGuard.Decision.RECOVER) {
                SessionLog.log(
                    "RENDERER_RECOVERY_START",
                    jsonObject("tabId" to tabId, "crashesInWindow" to crashGuard.crashesInWindow()),
                )
                manager.activeTabId?.let { rebuildTab(it, reason = "renderer_recovery") }
                ensurePrewarm()
                evictToLiveSet("renderer_recovery")
                updateHudCounts()
                SessionLog.log(
                    "RECOVERY_COMPLETE",
                    jsonObject(
                        "recreatedTabId" to manager.activeTabId,
                        "liveInstances" to live.size,
                        "crashesInWindow" to crashGuard.crashesInWindow(),
                    ),
                )
            } else {
                AppState.crashGuardTripped = true
                AppState.crashGuardDetail =
                    "The WebView renderer exited ${crashGuard.crashesInWindow()} times within " +
                        "${crashGuard.windowSeconds}s. Recovery is paused; use OK to dismiss."
                SessionLog.log(
                    "CRASH_GUARD_TRIPPED",
                    jsonObject(
                        "tabId" to tabId,
                        "crashesInWindow" to crashGuard.crashesInWindow(),
                        "windowSeconds" to crashGuard.windowSeconds,
                    ),
                )
            }
        }
        return true
    }

    // ---------------------------------------------------------------- session restore (§5.5)

    /**
     * Offers a stored session on Home (§5.5) when [allowOffer] holds (unclean
     * previous exit, no adb hooks steering this launch) AND the saved state
     * comes from a DIFFERENT process (a same-process state is not a "session
     * restore"). The file read runs on the LogIo thread (D-4).
     */
    fun offerRestoreAsync(allowOffer: () -> Boolean) {
        dev.tvbrowse.w0harness.log.LogIo.execute {
            val loaded = persistence.load()
            activity.runOnUiThread {
                if (loaded == null) return@runOnUiThread
                if (!allowOffer()) {
                    SessionLog.log("RESTORE_OFFER_SUPPRESSED", jsonObject("reason" to "launch gate"))
                    return@runOnUiThread
                }
                if (loaded.savedPid == android.os.Process.myPid()) {
                    SessionLog.log("RESTORE_OFFER_SUPPRESSED", jsonObject("reason" to "saved by this process"))
                    return@runOnUiThread
                }
                AppState.restoreOffer = AppState.RestoreOffer(
                    tabCount = loaded.tabCount,
                    activeUrl = loaded.activeUrl,
                    savedAtMs = loaded.savedAtMs,
                )
                SessionLog.log(
                    "RESTORE_OFFERED",
                    jsonObject(
                        "tabCount" to loaded.tabCount,
                        "activeUrl" to loaded.activeUrl,
                        "savedAtMs" to loaded.savedAtMs,
                        "savedPid" to loaded.savedPid,
                    ),
                )
            }
        }
    }

    /** Restores the persisted tab state and opens the active tab. */
    fun restoreSession() {
        val loaded = persistence.load() ?: run {
            SessionLog.log("RESTORE_FAILED", jsonObject("reason" to "no persisted state"))
            return
        }
        if (!manager.restoreFromJson(loaded.payload)) {
            SessionLog.log("RESTORE_FAILED", jsonObject("reason" to "payload unusable"))
            return
        }
        SessionLog.log(
            "RESTORE_APPLIED",
            jsonObject("tabs" to manager.tabCount, "activeTabId" to manager.activeTabId),
        )
        AppState.restoreOffer = null
        manager.activeTabId?.let { rebuildTab(it, reason = "session_restore") }
        updateHudCounts()
        AppState.screen = Screen.Browser
    }

    fun discardPersistedSession() {
        persistence.clear()
        AppState.restoreOffer = null
        SessionLog.log("RESTORE_DISCARDED", jsonObject())
    }

    /** Persists the app-owned tab state (async, D-4 thread) on every change. */
    fun persistTabState(trigger: String) {
        persistence.saveAsync(manager, trigger)
    }

    // ---------------------------------------------------------------- activity timers (§5.3, C-15)

    /** Background: global pauseTimers + cookie flush (never per-standby, C-15). */
    fun onActivityStopped() {
        SessionLog.log("WEBVIEW_TIMERS_PAUSED", jsonObject("reason" to "activity stopped"))
        runCatching {
            for (wv in live.values) wv.pauseTimers()
            prewarm?.pauseTimers()
            CookieManager.getInstance().flush()
        }
        persistTabState("activity_stopped")
    }

    /** Foreground: resumeTimers is global. */
    fun onActivityResumed() {
        runCatching {
            for (wv in live.values) wv.resumeTimers()
            prewarm?.resumeTimers()
        }
        SessionLog.log("WEBVIEW_TIMERS_RESUMED", jsonObject("reason" to "activity resumed"))
    }

    // ---------------------------------------------------------------- clients

    private inner class HarnessWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            AppState.currentUrl = url
            SessionLog.log("PAGE_STARTED", jsonObject("url" to url, "tabId" to viewToTab[view]))
            super.onPageStarted(view, url, favicon)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            // Top-level navigation bookkeeping (§5.5): push distinct URLs onto the
            // app-owned stack and persist (a redirect chain lands once it settles).
            val tabId = viewToTab[view]
            if (!isReload && url != null && tabId != null) {
                manager.updateTab(tabId) { it.pushUrl(url) }
                persistTabState("visited_history")
            }
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onPageFinished(view: WebView, url: String) {
            val tabId = viewToTab[view]
            SessionLog.log("PAGE_FINISHED", jsonObject("url" to url, "tabId" to tabId))
            if (tabId != null) {
                completeSwitchLatency(tabId, via = "page_finished")
                if (view === activeWebView) persistTabState("page_finished")
            }
            super.onPageFinished(view, url)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) {
                SessionLog.log(
                    "RECEIVED_ERROR",
                    jsonObject(
                        "url" to request.url.toString(),
                        "errorCode" to error.errorCode,
                        "description" to (error.description?.toString() ?: ""),
                        "tabId" to viewToTab[view],
                    ),
                )
            }
            super.onReceivedError(view, request, error)
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            // Local asset origin for the bundled M2 pages — no network (§1.5).
            return runCatching { assetLoader.shouldInterceptRequest(request.url) }.getOrNull()
                ?: super.shouldInterceptRequest(view, request)
        }

        override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
            return this@WebViewPool.onRenderProcessGoneInternal(view, detail)
        }
    }

    private inner class HarnessChromeClient : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            SessionLog.log(
                "CONSOLE",
                jsonObject(
                    "level" to consoleMessage.messageLevel()?.name,
                    "source" to consoleMessage.sourceId(),
                    "line" to consoleMessage.lineNumber(),
                    "message" to consoleMessage.message(),
                ),
            )
            return super.onConsoleMessage(consoleMessage)
        }

        /** Fullscreen (video or element) attaches OUTSIDE the Compose tree (D-gap-09). */
        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            SessionLog.log(
                "FULLSCREEN_SHOW",
                jsonObject(
                    "viewClass" to view.javaClass.name,
                    "widthPx" to view.layoutParams?.width,
                    "requestedFrom" to (AppState.currentUrl ?: ""),
                ),
            )
            fullscreenHost?.enterFullscreen(view, callback)
                ?: run {
                    SessionLog.log("FULLSCREEN_HOST_MISSING", jsonObject())
                    runCatching { callback.onCustomViewHidden() }
                }
        }

        override fun onHideCustomView() {
            val hidden = fullscreenHost?.exitFullscreen() ?: false
            SessionLog.log("FULLSCREEN_HIDE", jsonObject("overlayWasShowing" to hidden))
            super.onHideCustomView()
        }
    }

    private fun bundleApproxBytes(bundle: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.dataSize()
        } catch (t: BadParcelableException) {
            -1
        } finally {
            parcel.recycle()
        }
    }

    companion object {
        const val DESKTOP_USER_AGENT: String =
            "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"

        /** Origin that serves the bundled M2 pages through [WebViewAssetLoader]. */
        const val LOCAL_ASSET_BASE = "https://appassets.androidplatform.net/assets/"
    }
}
