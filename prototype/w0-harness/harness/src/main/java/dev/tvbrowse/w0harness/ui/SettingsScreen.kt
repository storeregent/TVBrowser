package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.web.WebViewPool

/**
 * Settings (brief §4.7). M1 rows are functional: activity variant (§3.2),
 * per-WebView Safe Browsing (§3.1) and desktop/mobile UA. Rows whose behaviour
 * ships in M2/M3 are displayed but disabled, and are still recorded in the
 * session header through SettingsStore.snapshot().
 */
@Composable
fun SettingsScreen(activity: android.content.Context, settings: SettingsStore, pool: WebViewPool) {
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
                    onClick = { },
                    headlineContent = { Text("Activity variant") },
                    supportingContent = {
                        Text(
                            "Locked = swallow config changes (default). Unlocked = recreate on config change (A-01). Applies on next launch.",
                            fontSize = 11.sp,
                            color = HarnessColors.MutedText,
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(if (variantUnlocked) "unlocked" else "locked", fontSize = 12.sp)
                            Switch(
                                checked = variantUnlocked,
                                onCheckedChange = { checked ->
                                    variantUnlocked = checked
                                    settings.setActivityVariant(
                                        if (checked) SettingsStore.VARIANT_UNLOCKED else SettingsStore.VARIANT_LOCKED,
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
                            )
                        }
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    headlineContent = { Text("Safe Browsing (per-WebView, default off)") },
                    supportingContent = {
                        Text(
                            "Applies to the live WebView immediately and to new WebViews. State before/after is logged (T-15 groundwork).",
                            fontSize = 11.sp,
                            color = HarnessColors.MutedText,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = safeBrowsing,
                            onCheckedChange = { checked ->
                                safeBrowsing = checked
                                settings.setSafeBrowsingEnabled(checked)
                                SessionLog.log(
                                    "SAFE_BROWSING_TOGGLED",
                                    jsonObject("requested" to checked),
                                )
                                pool.currentWebView?.let { pool.applySafeBrowsing(it) }
                            },
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    headlineContent = { Text("Desktop user agent") },
                    supportingContent = {
                        Text("Applied to new WebViews and future loads of the live one.", fontSize = 11.sp, color = HarnessColors.MutedText)
                    },
                    trailingContent = {
                        Switch(
                            checked = desktopUa,
                            onCheckedChange = { checked ->
                                desktopUa = checked
                                settings.setDesktopUserAgent(checked)
                                SessionLog.log(
                                    "SETTING_CHANGED",
                                    jsonObject(
                                        "key" to SettingsStore.KEY_DESKTOP_UA,
                                        "value" to checked,
                                        "ua" to if (checked) WebViewPool.DESKTOP_USER_AGENT else "default(webview)",
                                    ),
                                )
                                pool.currentWebView?.settings?.userAgentString =
                                    if (checked) WebViewPool.DESKTOP_USER_AGENT else null
                            },
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    headlineContent = { Text("Standby mode (M2)") },
                    supportingContent = { Text("Current: ${settings.standbyMode()} — functional in M2 (§5.3)", fontSize = 11.sp, color = HarnessColors.MutedText) },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    headlineContent = { Text("Popup policy (M3)") },
                    supportingContent = { Text("Current: policy ${settings.popupPolicy()} — functional in M3 (§5.6)", fontSize = 11.sp, color = HarnessColors.MutedText) },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    headlineContent = { Text("Crash-guard window (M2)") },
                    supportingContent = { Text("Current: ${settings.crashGuardWindowSeconds()}s — functional in M2 (§5.4)", fontSize = 11.sp, color = HarnessColors.MutedText) },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    headlineContent = { Text("Cursor mode (M3)") },
                    supportingContent = {
                        Text(
                            "Current: ${if (settings.cursorEnabled()) "on" else "off"} / ${settings.cursorInjectionMode()} — functional in M3 (§5.9)",
                            fontSize = 11.sp,
                            color = HarnessColors.MutedText,
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    headlineContent = { Text("Test blocklist (M3)") },
                    supportingContent = {
                        Text(
                            "Current: ${if (settings.testBlocklistEnabled()) "on" else "off"} (off = plain-WebView reference for P2) — functional in M3 (§5.7)",
                            fontSize = 11.sp,
                            color = HarnessColors.MutedText,
                        )
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { AppState.screen = Screen.Home },
                    headlineContent = { Text("Back to Home") },
                )
            }
        }
    }
}
