package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults

/**
 * Focus-visible defaults for every interactive element (M1.1 D-3):
 * a focused element gets ALL of
 *   1. a high-contrast bright container colour (blue #2684FF with near-black text),
 *   2. a 2 dp light border,
 *   3. a 1.05 scale,
 * so the focus state is obvious from 3 m against both the background and the
 * unfocused state.
 */
object TvFocus {

    // ---- tv-material Button ----

    @Composable
    fun buttonColors() = ButtonDefaults.colors(
        containerColor = HarnessColors.SurfaceRaised,
        contentColor = HarnessColors.OnSurface,
        focusedContainerColor = HarnessColors.FocusContainer,
        focusedContentColor = HarnessColors.OnFocusContainer,
        pressedContainerColor = HarnessColors.FocusContainer,
        pressedContentColor = HarnessColors.OnFocusContainer,
        disabledContainerColor = HarnessColors.SurfaceRaised.copy(alpha = 0.4f),
        disabledContentColor = HarnessColors.MutedText,
    )

    @Composable
    fun buttonScale() = ButtonDefaults.scale(focusedScale = 1.05f)

    @Composable
    fun buttonBorder() = ButtonDefaults.border(
        border = Border(border = BorderStroke(1.dp, HarnessColors.BorderUnfocused)),
        focusedBorder = Border(border = BorderStroke(2.dp, HarnessColors.BorderFocused)),
    )

    // ---- clickable Surface (rows, URL entry) ----

    @Composable
    fun surfaceColors() = ClickableSurfaceDefaults.colors(
        containerColor = HarnessColors.SurfaceRaised,
        contentColor = HarnessColors.OnSurface,
        focusedContainerColor = HarnessColors.FocusContainer,
        focusedContentColor = HarnessColors.OnFocusContainer,
        pressedContainerColor = HarnessColors.FocusContainer,
        pressedContentColor = HarnessColors.OnFocusContainer,
    )

    @Composable
    fun surfaceScale() = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)

    @Composable
    fun surfaceBorder() = ClickableSurfaceDefaults.border(
        border = Border(border = BorderStroke(1.dp, HarnessColors.BorderUnfocused)),
        focusedBorder = Border(border = BorderStroke(2.dp, HarnessColors.BorderFocused)),
    )

    // ---- ListItem (test URL list, settings rows) ----

    @Composable
    fun listItemColors(
        enabled: Boolean = true,
    ) = ListItemDefaults.colors(
        containerColor = if (enabled) HarnessColors.Surface else HarnessColors.Surface.copy(alpha = 0.5f),
        contentColor = if (enabled) HarnessColors.OnSurface else HarnessColors.MutedText,
        focusedContainerColor = HarnessColors.FocusContainer,
        focusedContentColor = HarnessColors.OnFocusContainer,
        disabledContainerColor = HarnessColors.Surface.copy(alpha = 0.5f),
        disabledContentColor = HarnessColors.MutedText,
    )

    fun listItemScale() = ListItemDefaults.scale(focusedScale = 1.05f)

    @Composable
    fun listItemBorder() = ListItemDefaults.border(
        border = Border(border = BorderStroke(1.dp, HarnessColors.BorderUnfocused)),
        focusedBorder = Border(border = BorderStroke(2.dp, HarnessColors.BorderFocused)),
    )
}

/** Shared button wrapper: tv-material Button with the D-3 focus styles applied. */
@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier,
        enabled = enabled,
        scale = TvFocus.buttonScale(),
        border = TvFocus.buttonBorder(),
        colors = TvFocus.buttonColors(),
        content = content,
    )
}
