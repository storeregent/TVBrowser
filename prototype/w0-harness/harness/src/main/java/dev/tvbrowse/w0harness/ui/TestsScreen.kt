package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.web.StandbyMode
import dev.tvbrowse.w0harness.web.WebViewPool

private data class TestEntry(
    val title: String,
    val detail: String,
    val milestone: String,
    val action: Int,
    val argument: String = "",
)

// Action ids: 0 none, 1 device info, 2 back home, 10+ = M2 triggers (§5.3/§5.4/§5.5)
private val ENTRIES = listOf(
    TestEntry("Device info dump", "T-01, T-02", "M1", 1),
    TestEntry("Back-key counters", "T-07 (counts in Device Info)", "M1", 1),
    TestEntry("Cold start & WebView timing", "T-18 (timings in Device Info)", "M1", 1),
    TestEntry("Lifecycle & key logger", "P1 logger part, T-11 logger part (passive; use Mark step)", "M1", 2),
    TestEntry("WebView guard", "T-17 (passive guard; error screen instead of crash)", "M1", 2),
    TestEntry("Tabs screen", "P3, T-05 — open/switch/close tabs, live/stored state", "M2", 3),
    TestEntry("Trigger renderer crash (chrome://crash)", "P4, T-03 — didCrash()==true recovery path", "M2", 10),
    TestEntry("Terminate renderer (didCrash()==false)", "P4, T-03 — WebViewRenderProcess.terminate()", "M2", 11),
    TestEntry("Crash-loop guard", "P4 — crash 3x within the window; the guard trips (§5.4)", "M2", 12),
    TestEntry("Open new tab (example.com)", "P3 — 2-live/10-stored accounting", "M2", 13),
    TestEntry("Switch to tab 1", "P3 — TAB_SWITCH latency (§5.3)", "M2", 14),
    TestEntry("Standby mode: prewarm / MRU / none", "T-05 — cycles the setting, live set follows", "M2", 15),
    TestEntry("Fullscreen test page (local)", "P1 fullscreen part — onShowCustomView container (D-gap-09)", "M2", 16),
    TestEntry("Offer session restore", "T-12 — shows the Restore-session dialog on Home", "M2", 17),
    TestEntry("Popup / new-window lab", "P8, T-06 — not in this build", "M3", 0),
    TestEntry("Service-worker probe + test blocklist", "P7 — not in this build", "M3", 0),
    TestEntry("Network & Safe Browsing lab", "P5, T-15 — not in this build (Safe Browsing toggle lives in Settings)", "M3", 0),
    TestEntry("Cursor mode (basic)", "T-08 — not in this build", "M3", 0),
)

/** Tests menu (brief §4.5): one entry per test module; M2 triggers are functional. */
@Composable
fun TestsScreen(pool: WebViewPool, settings: SettingsStore) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Tests", fontSize = 24.sp)
        Text(
            "M1/M2 entries are functional. M3 modules arrive in a later assignment.",
            fontSize = 12.sp,
            color = HarnessColors.MutedText,
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(ENTRIES.size) { index ->
                val entry = ENTRIES[index]
                ListItem(
                    selected = false,
                    enabled = entry.action != 0,
                    onClick = { onTestEntry(entry, pool, settings) },
                    colors = TvFocus.listItemColors(enabled = entry.action != 0),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text(entry.title) },
                    supportingContent = {
                        Text(
                            "[${entry.milestone}] ${entry.detail}",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
        }
    }
}

private fun onTestEntry(entry: TestEntry, pool: WebViewPool, settings: SettingsStore) {
    when (entry.action) {
        1 -> AppState.screen = Screen.DeviceInfo
        2 -> AppState.screen = Screen.Home
        3 -> AppState.screen = Screen.Tabs
        10 -> {
            AppState.screen = Screen.Browser
            pool.triggerCrashInActive()
        }
        11 -> {
            AppState.screen = Screen.Browser
            pool.terminateActiveRenderer()
        }
        12 -> {
            // Crash-loop guard demo: three chrome://crash triggers, back to back.
            AppState.screen = Screen.Browser
            pool.triggerCrashInActive()
            SessionLog.log(
                "CRASH_GUARD_DEMO",
                jsonObject("windowSeconds" to settings.crashGuardWindowSeconds(), "hint" to "run this entry 3x quickly; the 3rd trips the guard"),
            )
        }
        13 -> {
            AppState.screen = Screen.Browser
            pool.openTab("https://example.com/")
        }
        14 -> {
            val first = pool.manager.allTabs.firstOrNull()?.id
            if (first != null) {
                AppState.screen = Screen.Browser
                pool.switchTo(first, trigger = "tests_screen")
            } else {
                AppState.actionResult = "No tabs yet: open one first (URL + Go, or \"Open new tab\")."
            }
        }
        15 -> {
            val next = when (pool.manager.standbyMode) {
                StandbyMode.PREWARM -> StandbyMode.MRU
                StandbyMode.MRU -> StandbyMode.NONE
                StandbyMode.NONE -> StandbyMode.PREWARM
            }
            settings.setStandbyModeValue(next.id)
            pool.setStandbyMode(next, trigger = "tests_screen")
            AppState.actionResult = "Standby mode: ${next.id}"
        }
        16 -> {
            AppState.screen = Screen.Browser
            pool.loadUrl(WebViewPool.LOCAL_ASSET_BASE + "m2-fullscreen.html")
        }
        17 -> {
            pool.offerRestoreAsync { true }
            AppState.screen = Screen.Home
        }
    }
}
