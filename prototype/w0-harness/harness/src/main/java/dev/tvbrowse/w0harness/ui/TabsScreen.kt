package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import dev.tvbrowse.w0harness.web.WebViewPool

/**
 * Tabs screen (brief §4.3): all tabs with their live/stored state and the
 * active/standby role. Enter on a row switches to that tab (P3/T-05); the
 * button row opens/closes tabs. D-pad-first: no text inputs (M1.1 D-1 rules).
 */
@Composable
fun TabsScreen(pool: WebViewPool) {
    // Reading AppState counters subscribes this screen to pool updates.
    val tabCount = AppState.tabCount
    val liveCount = AppState.liveWebViewCount

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Tabs", fontSize = 24.sp)
        Text(
            "$tabCount/10 tabs · $liveCount live WebView(s) (max 2) · standby: ${pool.manager.standbyMode.id}",
            fontSize = 12.sp,
            color = HarnessColors.MutedText,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(onClick = { pool.openTab(null); AppState.screen = Screen.Browser }) { Text("New tab") }
            TvButton(onClick = { pool.closeActiveTab() }) { Text("Close active") }
            TvButton(onClick = { AppState.screen = Screen.Home }) { Text("Back to Home") }
        }

        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(pool.manager.allTabs.size) { index ->
                val tab = pool.manager.allTabs[index]
                val isActive = tab.id == pool.manager.activeTabId
                val isLive = !isActive && pool.manager.liveTabIds().contains(tab.id)
                val role = when {
                    isActive -> "active"
                    isLive -> "standby (live)"
                    else -> "stored"
                }
                ListItem(
                    selected = false,
                    onClick = {
                        pool.switchTo(tab.id, trigger = "tabs_screen")
                        AppState.screen = Screen.Browser
                    },
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text("Tab ${tab.id} · $role") },
                    supportingContent = {
                        Text(
                            (tab.currentUrl ?: "(blank)") +
                                " · stack ${tab.index + 1}/${tab.urlStack.size} · scrollY ${tab.scrollY}",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
            item {
                if (pool.manager.allTabs.isEmpty()) {
                    Text(
                        "No tabs yet. Open one from Home (URL + Go) or press \"New tab\".",
                        fontSize = 13.sp,
                        color = HarnessColors.MutedText,
                    )
                }
            }
        }
    }
}
