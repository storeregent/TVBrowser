package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.web.WebViewPool

/**
 * Browser (brief §4.2): one WebView in an imperative host FrameLayout, plus the
 * Compose HUD (URL, Back, Forward, Reload, Tabs count, Menu). M1.1: themed
 * focus-visible HUD buttons (D-2/D-3).
 *
 * D-pad focus inside the page uses the WebView's native focus navigation; the
 * HUD only exists while visible (Menu key / long-OK toggles it).
 */
@Composable
fun BrowserScreen(pool: WebViewPool, onNavigateBack: () -> Unit) {
    val hudFocusRequester = remember { FocusRequester() }

    LaunchedEffect(AppState.hudVisible) {
        if (AppState.hudVisible) {
            hudFocusRequester.requestFocus()
        } else {
            pool.focusPage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (AppState.hudVisible) {
            HudBar(pool, onNavigateBack, hudFocusRequester)
        }

        if (AppState.crashGuardTripped) {
            // §5.4 crash-loop guard: D-pad-dismissible error page instead of recreating.
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Renderer crash guard", fontSize = 24.sp)
                Text(
                    AppState.crashGuardDetail ?: "The renderer exited repeatedly.",
                    fontSize = 14.sp,
                    color = HarnessColors.MutedText,
                )
                Text(
                    "Recovery is paused so the crash loop cannot spin. The details were written to the " +
                        "session log (CRASH_GUARD_TRIPPED).",
                    fontSize = 12.sp,
                    color = HarnessColors.MutedText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvButton(onClick = { pool.resetCrashGuard() }) { Text("Reset guard & retry") }
                    TvButton(onClick = {
                        pool.resetCrashGuard()
                        AppState.screen = Screen.Home
                    }) { Text("Back to Home") }
                }
            }
        } else {
            val error = AppState.webViewError
            if (error != null) {
                // T-17 / §5.10 error screen (WebView provider unavailable).
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("WebView problem", fontSize = 24.sp)
                    Text(error, fontSize = 14.sp, color = HarnessColors.MutedText)
                    Text(
                        "This is the WebView guard error screen (T-17). The app did not crash. " +
                            "The details were written to the session log (WEBVIEW_UNAVAILABLE / RENDER_PROCESS_GONE).",
                        fontSize = 12.sp,
                        color = HarnessColors.MutedText,
                    )
                    TvButton(onClick = {
                        AppState.webViewError = null
                        AppState.screen = Screen.Home
                    }) { Text("Back to Home") }
                }
            } else {
                LaunchedEffect(Unit) {
                    pool.ensureCreated()
                }
                // pendingUrl may arrive while the Browser screen is already
                // composed (singleTask onNewIntent from an adb hook): observe it.
                LaunchedEffect(AppState.pendingUrl) {
                    AppState.pendingUrl?.let { pending ->
                        pool.loadUrl(pending)
                        AppState.pendingUrl = null
                    }
                }
                AndroidView(
                    factory = { pool.hostContainer },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HudBar(pool: WebViewPool, onNavigateBack: () -> Unit, focusRequester: FocusRequester) {
    // Reading the delegated state during composition subscribes to changes.
    val currentUrl = AppState.currentUrl
    Column(
        Modifier.fillMaxWidth().background(HarnessColors.HudBackground).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(onClick = onNavigateBack, focusRequester = focusRequester) { Text("Back") }
            TvButton(onClick = { pool.goForward() }) { Text("Fwd") }
            TvButton(onClick = { pool.reload() }) { Text("Reload") }
            // Tabs HUD element (§4.2): n/10 with the live instance count (M2 §5.3).
            TvButton(onClick = { AppState.screen = Screen.Tabs }) { Text("Tabs ${AppState.tabCount}/10 · ${AppState.liveWebViewCount} live") }
            Spacer(Modifier.weight(1f))
            TvButton(onClick = {
                AppState.hudVisible = false
                SessionLog.log("HUD_TOGGLED", jsonObject("visible" to false, "trigger" to "hud_button"))
            }) { Text("Hide (Menu)") }
        }
        Text(
            currentUrl ?: "(no page loaded)",
            fontSize = 13.sp,
            color = HarnessColors.MutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
