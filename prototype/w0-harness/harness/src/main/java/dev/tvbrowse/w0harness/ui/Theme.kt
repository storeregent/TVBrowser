package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import dev.tvbrowse.w0harness.util.HarnessPalette

/**
 * Harness colours (M1.1 D-2): all values come from [HarnessPalette], whose
 * body-text/background pairs are unit-tested to reach WCAG AA >= 4.5:1.
 */
object HarnessColors {
    val WindowBackground = Color(HarnessPalette.BACKGROUND)
    val Background = Color(HarnessPalette.BACKGROUND)
    val Surface = Color(HarnessPalette.SURFACE)
    val SurfaceRaised = Color(HarnessPalette.SURFACE_RAISED)
    val FocusContainer = Color(HarnessPalette.FOCUS_CONTAINER)
    val Primary = Color(HarnessPalette.PRIMARY)
    val OnFocusContainer = Color(HarnessPalette.ON_FOCUS_CONTAINER)
    val OnPrimary = Color(HarnessPalette.ON_PRIMARY)
    val OnBackground = Color(HarnessPalette.ON_BACKGROUND)
    val OnSurface = Color(HarnessPalette.ON_SURFACE)
    val MutedText = Color(HarnessPalette.MUTED_TEXT)
    val BorderUnfocused = Color(HarnessPalette.BORDER_UNFOCUSED)
    val BorderFocused = Color(HarnessPalette.BORDER_FOCUSED)

    // Kept for older call sites (browser HUD background).
    val HudBackground = Surface
    val HudAccent = FocusContainer
}

/**
 * Dark-only Compose-for-TV theme (founder decision F-H2, M1.1 D-2).
 *
 * The root is wrapped in a tv-material [Surface] providing
 * `LocalContentColor = onBackground`, so every Text without an explicit colour
 * is light-on-dark and readable at 10 ft.
 */
@Composable
fun HarnessTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = HarnessColors.Primary,
        onPrimary = HarnessColors.OnPrimary,
        primaryContainer = HarnessColors.Primary,
        onPrimaryContainer = HarnessColors.OnPrimary,
        secondary = HarnessColors.Primary,
        onSecondary = HarnessColors.OnPrimary,
        secondaryContainer = HarnessColors.SurfaceRaised,
        onSecondaryContainer = HarnessColors.OnSurface,
        tertiary = HarnessColors.Primary,
        onTertiary = HarnessColors.OnPrimary,
        tertiaryContainer = HarnessColors.SurfaceRaised,
        onTertiaryContainer = HarnessColors.OnSurface,
        background = HarnessColors.Background,
        onBackground = HarnessColors.OnBackground,
        surface = HarnessColors.Surface,
        onSurface = HarnessColors.OnSurface,
        surfaceVariant = HarnessColors.SurfaceRaised,
        onSurfaceVariant = HarnessColors.OnSurface,
        inverseSurface = HarnessColors.FocusContainer,
        inverseOnSurface = HarnessColors.OnFocusContainer,
        border = HarnessColors.BorderUnfocused,
        borderVariant = HarnessColors.BorderUnfocused,
    )
    MaterialTheme(colorScheme = scheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = HarnessColors.Background,
                contentColor = HarnessColors.OnBackground,
            ),
        ) {
            content()
        }
    }
}
