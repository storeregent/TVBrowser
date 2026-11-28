package dev.tvbrowse.w0harness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme

object HarnessColors {
    val WindowBackground = Color(0xFF0E1116)
    val HudBackground = Color(0xFF1A2230)
    val HudAccent = Color(0xFF2684FF)
    val MutedText = Color(0xFF9AA5B4)
}

/** Compose for TV theme (founder decision F-H2). */
@Composable
fun HarnessTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(HarnessColors.WindowBackground)) {
            content()
        }
    }
}
