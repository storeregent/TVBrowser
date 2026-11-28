package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
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
 * Home (brief §4.1): URL entry, bundled test URLs, and the buttons that reach
 * every other screen including "Mark step".
 */
@Composable
fun HomeScreen(activity: android.app.Activity, settings: SettingsStore) {
    var urlInput by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf("") }
    var showMarkDialog by rememberSaveable { mutableStateOf(false) }
    val testUrls = remember { TestUrls.load(activity) }

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
            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    inputError = ""
                },
                label = { Text("URL (plain text is not searched)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { openNormalized(activity, urlInput) { inputError = it } }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { openNormalized(activity, urlInput) { inputError = it } }) {
                Text("Go")
            }
        }
        if (inputError.isNotEmpty()) {
            Text(inputError, fontSize = 12.sp, color = HarnessColors.HudAccent)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { AppState.screen = Screen.Tests }) { Text("Tests") }
            Button(onClick = { AppState.screen = Screen.DeviceInfo }) { Text("Device Info") }
            Button(onClick = { AppState.screen = Screen.Logs }) { Text("Logs") }
            Button(onClick = { AppState.screen = Screen.Settings }) { Text("Settings") }
            Button(onClick = { showMarkDialog = true }) { Text("Mark step") }
        }

        Text("Test URLs (bundled assets/test-urls.json)", fontSize = 14.sp, color = HarnessColors.MutedText)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(testUrls.urls) { entry ->
                ListItem(
                    selected = false,
                    onClick = {
                        SessionLog.log("TEST_URL_SELECTED", jsonObject("url" to entry.url, "label" to entry.label))
                        AppState.pendingUrl = entry.url
                        AppState.screen = Screen.Browser
                    },
                    headlineContent = { Text(entry.label) },
                    supportingContent = { Text(entry.note ?: entry.url, fontSize = 11.sp, color = HarnessColors.MutedText) },
                )
            }
        }
        Spacer(Modifier.padding(2.dp))
        Text(
            "Zero harness-initiated network traffic: pages load only when you open them.",
            fontSize = 11.sp,
            color = HarnessColors.MutedText,
        )
    }

    if (showMarkDialog) {
        MarkStepDialog(
            onDismiss = { showMarkDialog = false },
            onMark = { label ->
                SessionLog.mark(label)
                showMarkDialog = false
            },
        )
    }
}

private fun openNormalized(context: android.content.Context, raw: String, onError: (String) -> Unit) {
    val url = UrlNormalizer.normalize(raw)
    if (url == null) {
        SessionLog.log("URL_ENTRY_IGNORED", jsonObject("input" to raw, "reason" to "does not parse as a URL; no search engine call"))
        onError("Ignored: not a URL (no search engine calls in the harness).")
        return
    }
    SessionLog.log("URL_ENTRY_ACCEPTED", jsonObject("input" to raw, "normalized" to url))
    AppState.pendingUrl = url
    AppState.screen = Screen.Browser
}
