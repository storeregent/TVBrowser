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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.info.DeviceInfoCollector
import dev.tvbrowse.w0harness.log.SessionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Device Info (brief §5.1, T-01/T-02): full device + WebView diagnostics,
 * T-07 back-key counters, T-18 timings, with an Export button that writes
 * device_info_<ts>.json into the log directory.
 */
@Composable
fun DeviceInfoScreen(activity: android.app.Activity) {
    val info = remember { DeviceInfoCollector.collect(activity) }
    val pretty = remember {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), info)
    }
    var status by remember { mutableStateOf("Not exported yet") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Device Info (T-01, T-02)", fontSize = 24.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val file = SessionLog.exportDeviceInfo(info)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        status = "Saved: ${file.absolutePath}"
                    }
                }
            }) { Text("Export") }
            Button(onClick = { dev.tvbrowse.w0harness.app.AppState.screen = Screen.Home }) { Text("Back to Home") }
        }
        Text(status, fontSize = 12.sp, color = HarnessColors.MutedText)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { Text(pretty, fontSize = 11.sp, color = HarnessColors.MutedText) }
        }
    }
}
