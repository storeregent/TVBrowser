package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * TV-safe single-line text entry (M1.1 D-1, the R-11 pattern from the Haier run).
 *
 *  - Outside edit mode this is a plain focusable [Surface] row showing the
 *    current value. It never consumes D-pad Up/Down/Left/Right: focus moves
 *    through it like through any button.
 *  - OK switches it into edit mode; the caller then focuses
 *    [editFieldFocusRequester] and shows the IME.
 *  - Inside edit mode a thin `onPreviewKeyEvent` wrapper intercepts D-pad
 *    Up/Down BEFORE the inner BasicTextField can consume them and calls
 *    [onExitEdit] (move focus to the next element, do not commit). IME Done
 *    calls [onCommit]. Back is handled by a screen-level BackHandler.
 */
@Composable
fun TvTextEntryRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    editing: Boolean,
    onEnterEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onCommit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    editFieldFocusRequester: FocusRequester? = null,
) {
    // Intercepts D-pad Up/Down BEFORE the inner text field can consume them (D-1).
    // Only while editing, and only for KeyDown: the KeyUp of the same press must
    // not exit again, and outside edit mode the row must pass D-pad through.
    val editKeyHandler: (KeyEvent) -> Boolean = { event ->
        if (editing && event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.DirectionDown, Key.DirectionUp -> {
                    onExitEdit()
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    Surface(
        onClick = onEnterEdit,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent(editKeyHandler),
        colors = TvFocus.surfaceColors(),
        scale = TvFocus.surfaceScale(),
        border = TvFocus.surfaceBorder(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                fontSize = 13.sp,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 12.dp),
            )
            if (!editing) {
                Text(
                    if (value.isBlank()) placeholder else value,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = LocalContentColor.current.copy(alpha = if (value.isBlank()) 0.6f else 1f),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onCommit() }),
                        textStyle = TextStyle(color = LocalContentColor.current, fontSize = 16.sp),
                        cursorBrush = SolidColor(LocalContentColor.current),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (editFieldFocusRequester != null) {
                                    Modifier.focusRequester(editFieldFocusRequester)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}
