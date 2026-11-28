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
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen

private data class TestEntry(val title: String, val detail: String, val milestone: String, val action: Int)

private val ENTRIES = listOf(
    // Action 0 = none, 1 = device info, 2 = back to home info
    TestEntry("Device info dump", "T-01, T-02", "M1", 1),
    TestEntry("Back-key counters", "T-07 (counts in Device Info)", "M1", 1),
    TestEntry("Cold start & WebView timing", "T-18 (timings in Device Info)", "M1", 1),
    TestEntry("Lifecycle & key logger", "P1 logger part, T-11 logger part (passive; use Mark step)", "M1", 2),
    TestEntry("WebView guard", "T-17 (passive guard; error screen instead of crash)", "M1", 2),
    TestEntry("Tab manager + standby modes", "P3, T-05 — not in this build", "M2", 0),
    TestEntry("Renderer crash recovery + triggers", "P4, T-03 — not in this build", "M2", 0),
    TestEntry("Session restore after process death", "T-12 — not in this build", "M2", 0),
    TestEntry("Fullscreen video container", "P1 fullscreen part — not in this build", "M2", 0),
    TestEntry("Popup / new-window lab", "P8, T-06 — not in this build", "M3", 0),
    TestEntry("Service-worker probe + test blocklist", "P7 — not in this build", "M3", 0),
    TestEntry("Network & Safe Browsing lab", "P5, T-15 — not in this build (Safe Browsing toggle lives in Settings)", "M3", 0),
    TestEntry("Cursor mode (basic)", "T-08 — not in this build", "M3", 0),
)

/** Tests menu (brief §4.5): one entry per test module; unimplemented modules stay visible but disabled. */
@Composable
fun TestsScreen() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Tests", fontSize = 24.sp)
        Text(
            "M1 build: entries marked M1 are functional. M2/M3 modules arrive in later assignments.",
            fontSize = 12.sp,
            color = HarnessColors.MutedText,
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(ENTRIES.size) { index ->
                val entry = ENTRIES[index]
                ListItem(
                    selected = false,
                    enabled = entry.action != 0,
                    onClick = {
                        when (entry.action) {
                            1 -> AppState.screen = Screen.DeviceInfo
                            2 -> AppState.screen = Screen.Home
                        }
                    },
                    headlineContent = { Text(entry.title) },
                    supportingContent = { Text("[${entry.milestone}] ${entry.detail}", fontSize = 11.sp, color = HarnessColors.MutedText) },
                )
            }
        }
    }
}
