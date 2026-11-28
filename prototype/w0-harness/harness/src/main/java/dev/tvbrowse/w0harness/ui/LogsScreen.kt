package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.log.SessionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logs screen (brief §6): shows the absolute log directory path, the session
 * files, and the "Export all" button that writes a single zip into the same
 * directory (pull instructions are in the README). M1.1: dark theme (D-2),
 * visible focus (D-3), async export (D-4), adb action result line (D-5).
 */
@Composable
fun LogsScreen(activity: android.app.Activity) {
    var files by remember { mutableStateOf(SessionLog.listLogFiles()) }
    var status by remember { mutableStateOf(AppState.actionResult ?: "") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Logs", fontSize = 24.sp)
        Text("Log directory:", fontSize = 12.sp, color = HarnessColors.MutedText)
        Text(SessionLog.logsDirectoryPath(), fontSize = 12.sp)
        Text("Current session file: ${SessionLog.sessionFilePath()}", fontSize = 12.sp, color = HarnessColors.MutedText)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val zip = SessionLog.exportAll()
                    files = SessionLog.listLogFiles()
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        AppState.actionResult = "Exported: ${zip.absolutePath}"
                        status = "Exported: ${zip.absolutePath}"
                    }
                }
            }) { Text("Export all") }
            TvButton(onClick = { AppState.screen = Screen.Home }) { Text("Back to Home") }
        }
        if (status.isNotEmpty()) Text(status, fontSize = 12.sp, color = HarnessColors.FocusContainer)

        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(files) { file ->
                ListItem(
                    selected = false,
                    onClick = { },
                    colors = TvFocus.listItemColors(),
                    scale = TvFocus.listItemScale(),
                    border = TvFocus.listItemBorder(),
                    headlineContent = { Text(file.name, fontSize = 13.sp) },
                    supportingContent = {
                        Text(
                            "${"%.1f".format(file.length() / 1024.0)} KiB · ${
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(file.lastModified()))
                            }",
                            fontSize = 11.sp,
                            color = androidx.tv.material3.LocalContentColor.current.copy(alpha = 0.75f),
                        )
                    },
                )
            }
        }
        Text(
            "Pull with: adb pull " + SessionLog.logsDirectoryPath().removePrefix("/storage/emulated/") + " <local dir>  " +
                "(see harness README for the exact path)",
            fontSize = 11.sp,
            color = HarnessColors.MutedText,
        )
    }
}
