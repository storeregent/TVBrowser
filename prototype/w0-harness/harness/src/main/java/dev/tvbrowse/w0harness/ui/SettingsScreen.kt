package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.web.WebViewPool

/**
 * Settings (brief §4.7, M1.1 D-1/D-2/D-3).
 *
 * Every row is a single focusable ListItem (row click = toggle; the Switch is a
 * decorative `onCheckedChange = null` so the D-pad can never get trapped between
 * two focusables in one row). No text inputs on this screen.
 */
@Composable
fun SettingsScreen(activity: android.app.Activity, settings: SettingsStore, pool: WebViewPool) {
    var variantUnlocked by remember { mutableStateOf(settings.activityVariant() == SettingsStore.VARIANT_UNLOCKED) }
    var safeBrowsing by remember { mutableStateOf(settings.safeBrowsingEnabled()) }
    var desktopUa by remember { mutableStateOf(settings.desktopUserAgent()) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Settings", fontSize = 24.sp)

        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item {
                ListItem(
                    selected = false,
                    onClick = {
                        variantUnlocked = !variantUnlocked
                        settings.setActivityVariant(
                            if (variantUnlocked) SettingsStore.VARIANT_UNLOCKED else SettingsStore.VARIANT_LOCKED,
                        )
                        SessionLog.log(
                            "SETTING_CHANGED",
                            jsonObject(
                                "key" to SettingsStore.KEY_ACTIVITY_VARIANT,
                                "value" to settings.activityVariant(),
                                "appliesOn" to "next_launch",
                            ),
                        )
                    },
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text("Activity variant") },
                    supportingContent = {
                        Text(
                            if (variantUnlocked) {
                                "unlocked: recreate on config change (A-01). Applies on next launch."
                            } else {
                                "locked: swallow config changes (default, FR-14). Applies on next launch."
                            },
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                    trailingContent = { Switch(checked = variantUnlocked, onCheckedChange = null) },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = {
                        safeBrowsing = !safeBrowsing
                        settings.setSafeBrowsingEnabled(safeBrowsing)
                        SessionLog.log("SAFE_BROWSING_TOGGLED", jsonObject("requested" to safeBrowsing))
                        pool.currentWebView?.let { pool.applySafeBrowsing(it) }
                    },
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text("Safe Browsing (per-WebView, default off)") },
                    supportingContent = {
                        Text(
                            "Applies to the live WebView immediately and to new WebViews. State before/after is logged (T-15 groundwork).",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                    trailingContent = { Switch(checked = safeBrowsing, onCheckedChange = null) },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = {
                        desktopUa = !desktopUa
                        settings.setDesktopUserAgent(desktopUa)
                        SessionLog.log(
                            "SETTING_CHANGED",
                            jsonObject(
                                "key" to SettingsStore.KEY_DESKTOP_UA,
                                "value" to desktopUa,
                                "ua" to if (desktopUa) WebViewPool.DESKTOP_USER_AGENT else "default(webview)",
                            ),
                        )
                        pool.currentWebView?.settings?.userAgentString =
                            if (desktopUa) WebViewPool.DESKTOP_USER_AGENT else null
                    },
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text("Desktop user agent") },
                    supportingContent = {
                        Text(
                            "Applied to the live WebView immediately and to new WebViews.",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                    trailingContent = { Switch(checked = desktopUa, onCheckedChange = null) },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    colors = TvFocus.listItemColors(enabled = false),
                    headlineContent = { Text("Standby mode (M2)") },
                    supportingContent = {
                        Text(
                            "Current: ${settings.standbyMode()} — functional in M2 (§5.3)",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    colors = TvFocus.listItemColors(enabled = false),
                    headlineContent = { Text("Popup policy (M3)") },
                    supportingContent = {
                        Text(
                            "Current: policy ${settings.popupPolicy()} — functional in M3 (§5.6)",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    colors = TvFocus.listItemColors(enabled = false),
                    headlineContent = { Text("Crash-guard window (M2)") },
                    supportingContent = {
                        Text(
                            "Current: ${settings.crashGuardWindowSeconds()}s — functional in M2 (§5.4)",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    colors = TvFocus.listItemColors(enabled = false),
                    headlineContent = { Text("Cursor mode (M3)") },
                    supportingContent = {
                        Text(
                            "Current: ${if (settings.cursorEnabled()) "on" else "off"} / ${settings.cursorInjectionMode()} — functional in M3 (§5.9)",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    colors = TvFocus.listItemColors(enabled = false),
                    headlineContent = { Text("Test blocklist (M3)") },
                    supportingContent = {
                        Text(
                            "Current: ${if (settings.testBlocklistEnabled()) "on" else "off"} (off = plain-WebView reference for P2) — functional in M3 (§5.7)",
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { AppState.screen = Screen.Home },
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text("Back to Home") },
                )
            }
        }
    }
}
