package dev.tvbrowse.w0harness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
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

/**
 * Mark-step dialog (M1.1 D-1/D-2): no material3, dark tv-material Surface, and
 * the text input is never the initial focus (first preset is) and never traps
 * the D-pad (same TvTextEntryRow pattern as Home).
 */
@Composable
fun MarkStepDialog(onDismiss: () -> Unit) {
    var custom by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    val ime = LocalSoftwareKeyboardController.current

    val presetFocus = remember { FocusRequester() }
    val editFieldFocus = remember { FocusRequester() }
    val markCustomFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        presetFocus.requestFocus() // initial focus: first preset, not the text field
    }

    LaunchedEffect(editing) {
        if (editing) {
            editFieldFocus.requestFocus()
            ime?.show()
        }
    }

    BackHandler(enabled = editing) {
        editing = false
        ime?.hide()
        markCustomFocus.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = SurfaceDefaults.colors(
                containerColor = HarnessColors.Surface,
                contentColor = HarnessColors.OnSurface,
            ),
        ) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Mark step", fontSize = 20.sp)
                Text("Presets:", fontSize = 12.sp, color = HarnessColors.MutedText)
                PRESET_LABELS.forEachIndexed { index, label ->
                    TvButton(
                        onClick = {
                            SessionLog.mark(label)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = if (index == 0) presetFocus else null,
                    ) { Text(label, fontSize = 12.sp) }
                }
                TvTextEntryRow(
                    label = "Custom",
                    value = custom,
                    onValueChange = { custom = it },
                    editing = editing,
                    onEnterEdit = { editing = true },
                    onExitEdit = {
                        editing = false
                        ime?.hide()
                        markCustomFocus.requestFocus()
                    },
                    onCommit = {
                        val label = custom.trim()
                        if (label.isNotEmpty()) SessionLog.mark(label)
                        onDismiss()
                    },
                    placeholder = "Press OK to type a label",
                    modifier = Modifier.fillMaxWidth(),
                    editFieldFocusRequester = editFieldFocus,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvButton(
                        onClick = {
                            val label = custom.trim()
                            if (label.isNotEmpty()) SessionLog.mark(label)
                            onDismiss()
                        },
                        focusRequester = markCustomFocus,
                    ) { Text("Mark custom") }
                    TvButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
