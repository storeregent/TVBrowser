package dev.tvbrowse.w0harness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.BuildConfig
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.info.TestUrls
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.UrlNormalizer
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Home (brief §4.1, M1.1 D-1/D-2/D-6).
 *
 * D-pad contract:
 *  - Initial focus is the FIRST TEST URL (or the "Device Info" button when the
 *    list is empty) — never a text input.
 *  - The URL entry is TV-safe: OK enters edit mode, IME Done commits, Back /
 *    D-pad Up/Down leave edit mode without loading.
 *  - Traversal order: URL entry → Go → button row (Device Info, Logs, Settings,
 *    Tests, Mark step) → Test URL list; Up returns in reverse.
 */
@Composable
fun HomeScreen(activity: android.app.Activity, settings: SettingsStore) {
    var urlInput by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var inputError by rememberSaveable { mutableStateOf("") }
    var showMarkDialog by rememberSaveable { mutableStateOf(false) }
    val testUrls = remember { TestUrls.load(activity) }
    val ime = LocalSoftwareKeyboardController.current

    val urlRowFocus = remember { FocusRequester() }
    val goFocus = remember { FocusRequester() }
    val deviceInfoFocus = remember { FocusRequester() }
    val firstUrlFocus = remember { FocusRequester() }
    val editFieldFocus = remember { FocusRequester() }

    // A1: initial focus is a non-text element, requested once after first composition.
    LaunchedEffect(testUrls) {
        withFrameNanos { }
        if (testUrls.urls.isNotEmpty()) {
            firstUrlFocus.requestFocus()
        } else {
            deviceInfoFocus.requestFocus()
        }
    }

    // Edit mode: focus the inner text field and open the IME.
    LaunchedEffect(editing) {
        if (editing) {
            editFieldFocus.requestFocus()
            ime?.show()
        }
    }

    // Back while editing exits edit mode WITHOUT loading (D-1 / A3).
    BackHandler(enabled = editing) {
        editing = false
        ime?.hide()
        SessionLog.log(
            "BACK_DISPATCH",
            jsonObject("screen" to "Home", "canGoBack" to (urlInput.isNotBlank()), "handledBy" to "url_edit_exit", "loaded" to false),
        )
        goFocus.requestFocus()
    }

    fun commitUrl() { // Go button and IME Done
        val raw = urlInput.trim()
        if (raw.isEmpty()) { // D-6: empty Go does nothing and shows no error
            editing = false
            ime?.hide()
            return
        }
        val url = UrlNormalizer.normalize(raw)
        if (url == null) {
            SessionLog.log(
                "URL_ENTRY_IGNORED",
                jsonObject("input" to raw, "reason" to "does not parse as a URL; no search engine call"),
            )
            inputError = "Ignored: not a URL (no search engine calls in the harness)."
            return
        }
        editing = false
        ime?.hide()
        inputError = ""
        SessionLog.log("URL_ENTRY_ACCEPTED", jsonObject("input" to raw, "normalized" to url))
        AppState.pendingUrl = url
        AppState.screen = Screen.Browser
    }

    fun exitEditWithoutCommit() {
        editing = false
        ime?.hide()
        goFocus.requestFocus()
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("TVBrowse W0 Harness", fontSize = 26.sp)
        Text(
            "v${BuildConfig.VERSION_NAME} · flavor ${BuildConfig.FLAVOR} · git ${BuildConfig.GIT_SHA} · session ${SessionLog.sessionId()}",
            fontSize = 12.sp,
            color = HarnessColors.MutedText,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvTextEntryRow(
                label = "URL",
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    inputError = ""
                },
                editing = editing,
                onEnterEdit = { editing = true },
                onExitEdit = { exitEditWithoutCommit() },
                onCommit = { commitUrl() },
                placeholder = "Press OK to type a URL",
                modifier = Modifier.weight(1f),
                focusRequester = urlRowFocus,
                editFieldFocusRequester = editFieldFocus,
            )
            TvButton(onClick = { commitUrl() }, focusRequester = goFocus) { Text("Go") }
        }
        if (inputError.isNotEmpty()) {
            Text(inputError, fontSize = 12.sp, color = HarnessColors.FocusContainer)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(onClick = { AppState.screen = Screen.DeviceInfo }, focusRequester = deviceInfoFocus) { Text("Device Info") }
            TvButton(onClick = { AppState.screen = Screen.Logs }) { Text("Logs") }
            TvButton(onClick = { AppState.screen = Screen.Settings }) { Text("Settings") }
            TvButton(onClick = { AppState.screen = Screen.Tests }) { Text("Tests") }
            TvButton(onClick = { showMarkDialog = true }) { Text("Mark step") }
        }

        Text("Test URLs (bundled assets/test-urls.json)", fontSize = 14.sp, color = HarnessColors.MutedText)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(testUrls.urls) { index, entry ->
                ListItem(
                    selected = false,
                    onClick = {
                        SessionLog.log("TEST_URL_SELECTED", jsonObject("url" to entry.url, "label" to entry.label))
                        AppState.pendingUrl = entry.url
                        AppState.screen = Screen.Browser
                    },
                    modifier = if (index == 0) Modifier.focusRequester(firstUrlFocus) else Modifier,
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text(entry.label) },
                    supportingContent = {
                        Text(
                            entry.note ?: entry.url,
                            fontSize = 11.sp,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
        }

        // Result line for adb-driven actions (D-5): export paths, marks, etc.
        AppState.actionResult?.let { result ->
            Text(result, fontSize = 12.sp, color = HarnessColors.FocusContainer)
        }
        Text(
            "Zero harness-initiated network traffic: pages load only when you open them.",
            fontSize = 11.sp,
            color = HarnessColors.MutedText,
        )
    }

    if (showMarkDialog) {
        MarkStepDialog(onDismiss = { showMarkDialog = false })
    }
}
