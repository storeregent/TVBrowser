package dev.tvbrowse.w0harness.ui

import android.app.Activity
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.web.WebViewPool

/**
 * Screen router for both browser activity variants (brief §4). All screens are
 * D-pad-first: no touch is required anywhere.
 */
@Composable
fun HarnessRoot(
    activity: Activity,
    pool: WebViewPool,
    settings: SettingsStore,
    onNavigateBack: () -> Unit,
) {
    HarnessTheme {
        // Cold start: Application.onCreate -> first Home frame (T-18, brief §5.1).
        LaunchedEffect(Unit) {
            if (AppState.coldStartFirstFrameMs == null && AppState.appStartElapsedMs > 0) {
                val elapsed = SystemClock.elapsedRealtime() - AppState.appStartElapsedMs
                AppState.coldStartFirstFrameMs = elapsed
                runCatching { activity.reportFullyDrawn() }
                SessionLog.log(
                    "COLD_START_FIRST_FRAME",
                    jsonObject(
                        "elapsedMs" to elapsed,
                        "method" to "reportFullyDrawn after first Home composition",
                    ),
                )
            }
        }
        when (AppState.screen) {
            Screen.Home -> HomeScreen(activity, settings, pool)
            Screen.Browser -> BrowserScreen(pool, onNavigateBack)
            Screen.Tabs -> TabsScreen(pool)
            Screen.DeviceInfo -> DeviceInfoScreen(activity)
            Screen.Logs -> LogsScreen(activity)
            Screen.Settings -> SettingsScreen(activity, settings, pool)
            Screen.Tests -> TestsScreen(pool, settings)
        }
    }
}
