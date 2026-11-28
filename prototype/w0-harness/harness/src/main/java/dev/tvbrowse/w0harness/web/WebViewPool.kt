package dev.tvbrowse.w0harness.web

import android.annotation.SuppressLint
import android.app.Activity
import android.os.SystemClock
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import dev.tvbrowse.w0harness.BuildConfig
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.log.SessionLog

/**
 * Imperative WebView host (brief §3 / AA §1.2).
 *
 * The Compose tree sees exactly one plain [FrameLayout] (via a single
 * AndroidView). WebViews are created, attached, detached and destroyed here —
 * never inside a composable factory.
 *
 * M1 keeps exactly one live WebView (single-tab browsing). The full 2-live
 * tab pool (active + standby) is the M2 tab manager (brief §5.3); this class
 * is deliberately structured so the pool can grow to 2 live instances then.
 */
class WebViewPool(private val activity: Activity, private val settings: SettingsStore) {

    /** The single container exposed to Compose. Outlives screen navigation. */
    val hostContainer: FrameLayout = FrameLayout(activity)

    private var webView: WebView? = null

    val currentWebView: WebView? get() = webView

    // ---------------------------------------------------------------- creation

    /**
     * Creates + attaches the active WebView, guarded (T-17, §5.10): if the
     * provider is missing or disabled the app shows an error screen and logs
     * WEBVIEW_UNAVAILABLE instead of crashing.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun ensureCreated(): Boolean {
        if (webView != null) return true
        AppState.webViewError = null

        val provider = runCatching { WebViewCompat.getCurrentWebViewPackage(activity) }.getOrNull()
        if (provider == null) {
            SessionLog.log("WEBVIEW_PROVIDER_NULL", jsonObject("hint" to "getCurrentWebViewPackage returned null; provider likely missing"))
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
            return false
        }
        val durationMs = SystemClock.uptimeMillis() - start
        val isFirst = AppState.firstWebViewConstructorMs == null
        if (isFirst) AppState.firstWebViewConstructorMs = durationMs

        configure(created)
        webView = created
        hostContainer.addView(
            created,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        SessionLog.log(
            "WEBVIEW_CREATED",
            jsonObject(
                "constructorDurationMs" to durationMs,
                "isFirstConstructor" to isFirst,
                "providerPackage" to provider?.packageName,
                "providerVersion" to provider?.versionName,
                "liveInstances" to 1,
            ),
        )
        return true
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

    // ---------------------------------------------------------------- actions

    fun loadUrl(url: String) {
        val wv = webView ?: run {
            SessionLog.log("LOAD_URL_SKIPPED", jsonObject("url" to url, "reason" to "no live WebView"))
            return
        }
        SessionLog.log("LOAD_URL", jsonObject("url" to url))
        wv.loadUrl(url)
    }

    fun goBack() {
        val wv = webView ?: return
        if (wv.canGoBack()) {
            SessionLog.log("WEB_GO_BACK", jsonObject("target" to previousUrl(wv)))
            wv.goBack()
        }
    }

    fun goForward() {
        val wv = webView ?: return
        if (wv.canGoForward()) {
            SessionLog.log("WEB_GO_FORWARD", jsonObject())
            wv.goForward()
        }
    }

    fun reload() {
        val wv = webView ?: return
        SessionLog.log("WEB_RELOAD", jsonObject("url" to (AppState.currentUrl ?: "")))
        wv.reload()
    }

    /** System/HUD back inside the browser. Returns true when the WebView consumed it. */
    fun handleBack(): Boolean {
        val wv = webView ?: return false
        return if (wv.canGoBack()) {
            SessionLog.log("WEB_GO_BACK", jsonObject("target" to previousUrl(wv), "trigger" to "back_key"))
            wv.goBack()
            true
        } else {
            false
        }
    }

    fun focusPage() {
        webView?.requestFocus()
    }

    private fun previousUrl(wv: WebView): String? = runCatching {
        val list = wv.copyBackForwardList()
        val idx = list.currentIndex - 1
        if (idx >= 0) list.getItemAtIndex(idx)?.url else null
    }.getOrNull()

    // ---------------------------------------------------------------- teardown

    /** AA §6.3 sequence (M1 single instance): remove from parent -> stopLoading -> destroy. */
    fun destroyAll(reason: String) {
        webView?.let { wv ->
            SessionLog.log("WEBVIEW_DESTROY", jsonObject("reason" to reason))
            (wv.parent as? FrameLayout)?.removeView(wv)
            wv.stopLoading()
            runCatching { wv.destroy() }
        }
        webView = null
    }

    // ---------------------------------------------------------------- clients

    private inner class HarnessWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            AppState.currentUrl = url
            SessionLog.log("PAGE_STARTED", jsonObject("url" to url))
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            SessionLog.log("PAGE_FINISHED", jsonObject("url" to url))
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
                        "code" to error.code,
                        "description" to (error.description?.toString() ?: ""),
                    ),
                )
            }
            super.onReceivedError(view, request, error)
        }

        /**
         * Minimal safe cleanup for M1: log didCrash()/rendererPriorityAtExit(),
         * remove + destroy the dead instance, surface an error screen, return
         * true (AA-recommended handling). The full recreate-from-stored-state +
         * crash-loop guard is the M2 renderer recovery module (§5.4).
         */
        override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
            SessionLog.log(
                "RENDER_PROCESS_GONE",
                jsonObject(
                    "didCrash" to detail.didCrash(),
                    "rendererPriorityAtExit" to detail.rendererPriorityAtExit(),
                    "milestoneNote" to "M1 minimal cleanup; full recovery is M2",
                ),
            )
            activity.runOnUiThread {
                destroyAll("renderer_gone")
                AppState.webViewError = "The WebView renderer exited (didCrash=${detail.didCrash()})."
            }
            return true
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
    }

    companion object {
        const val DESKTOP_USER_AGENT: String =
            "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
}
