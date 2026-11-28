package dev.tvbrowse.w0harness.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Screens of the harness (brief §4). */
enum class Screen { Home, Browser, Tabs, DeviceInfo, Logs, Settings, Tests }

/**
 * Shared UI/session state. The harness has one browser activity alive at a time
 * (EntryActivity routes and finishes), so an application-scoped singleton is safe.
 */
object AppState {
    var screen by mutableStateOf(Screen.Home)
    var hudVisible by mutableStateOf(true)
    var currentUrl by mutableStateOf<String?>(null)
    var pendingUrl by mutableStateOf<String?>(null)

    /** Set when the WebView guard refused to create a WebView, or the renderer died (T-17). */
    var webViewError by mutableStateOf<String?>(null)

    /** Result line for adb-driven actions (M1.1 D-5), shown on Home / target screens. */
    var actionResult by mutableStateOf<String?>(null)

    /** SystemClock.elapsedRealtime() captured in Application.onCreate (T-18). */
    var appStartElapsedMs: Long = 0L

    /** Cold start: Application.onCreate -> first Home frame via reportFullyDrawn() (T-18). */
    var coldStartFirstFrameMs: Long? = null

    /** First WebView constructor duration (T-18). */
    var firstWebViewConstructorMs: Long? = null

    /** T-07 counters (brief §5.1). */
    var backKeyEventsSeen: Int = 0
    var backCallbackInvocations: Int = 0
    var hudToggles: Int = 0

    // ------------------------------------------------------------------ M2 (brief §5.3–§5.5)

    /** HUD "Tabs n/10" live count; updated by the pool on every tab change. */
    var tabCount by mutableStateOf(0)
    var liveWebViewCount by mutableStateOf(0)

    /** Crash-guard tripped: the renderer crashed more often than the guard allows (§5.4). */
    var crashGuardTripped by mutableStateOf(false)
    var crashGuardDetail by mutableStateOf<String?>(null)

    /** Session-restore offer (§5.5): non-null shows the D-pad dialog on Home. */
    var restoreOffer by mutableStateOf<RestoreOffer?>(null)

    /** True when Application.onCreate found the previous process's session_open marker. */
    var previousSessionDied: Boolean = false

    data class RestoreOffer(val tabCount: Int, val activeUrl: String?, val savedAtMs: Long)
}
