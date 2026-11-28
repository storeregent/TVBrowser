package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.tvbrowse.w0harness.log.SessionLog

/** Preset Mark-step labels (P1 steps), plus free text (brief §6). */
private val PRESET_LABELS = listOf(
    "P1: remote asleep",
    "P1: remote awake",
    "P1: mouse plugged",
    "P1: mouse unplugged",
    "P1: BT remote paired",
    "P1: BT remote disconnected",
    "P1: config change about to fire",
    "P3: switch start",
    "P3: switch end",
    "app backgrounded",
    "app foregrounded",
)

@Composable
fun MarkStepDialog(onDismiss: () -> Unit, onMark: (String) -> Unit) {
    var custom by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark step") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Presets:")
                PRESET_LABELS.forEach { label ->
                    Row(Modifier.fillMaxWidth()) {
                        Button(onClick = { SessionLog.mark(label); onDismiss() }) { Text(label, fontSize = 12.sp) }
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Custom label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = custom.trim()
                    if (label.isNotEmpty()) SessionLog.mark(label)
                    onDismiss()
                },
            ) { Text("Mark custom") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
    )
}
