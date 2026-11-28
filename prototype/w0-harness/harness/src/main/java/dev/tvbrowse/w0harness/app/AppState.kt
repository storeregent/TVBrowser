package dev.tvbrowse.w0harness.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Screens of the harness (brief §4). */
enum class Screen { Home, Browser, DeviceInfo, Logs, Settings, Tests }

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

    /** SystemClock.elapsedRealtime() captured in Application.onCreate (T-18). */
    var appStartElapsedMs: Long = 0L

    /** Cold start: Application.onCreate -> first Home frame via reportFullyDrawn() (T-18). */
    var coldStartFirstFrameMs: Long? = null

    /** First WebView constructor duration (T-18). */
    var firstWebViewConstructorMs: Long? = null

    // T-07 counters (brief §5.1).
    var backKeyEventsSeen: Int = 0
    var backCallbackInvocations: Int = 0
    var hudToggles: Int = 0
}
