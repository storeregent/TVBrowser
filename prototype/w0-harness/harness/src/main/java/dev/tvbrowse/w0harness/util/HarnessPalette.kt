package dev.tvbrowse.w0harness.util

/**
 * Harness palette + WCAG contrast checks (M1.1 D-2/D-3).
 *
 * Plain-JVM colour constants (no Compose imports) so the unit tests can prove
 * that every body-text/background pair used by the UI reaches WCAG AA (>= 4.5:1).
 * `ui/HarnessColors` derives its Compose `Color`s from these constants, so code
 * and evidence cannot drift apart.
 */
object HarnessPalette {
    // Surfaces
    const val BACKGROUND: Int = 0xFF0E1116.toInt()        // window background
    const val SURFACE: Int = 0xFF1A2230.toInt()           // cards, HUD, list rows
    const val SURFACE_RAISED: Int = 0xFF1D2733.toInt()    // button container (unfocused)

    // Focus / primary
    const val FOCUS_CONTAINER: Int = 0xFF2684FF.toInt()   // focused container (bright blue)
    const val PRIMARY: Int = 0xFF1B5FD9.toInt()           // primary action container
    const val ON_FOCUS_CONTAINER: Int = 0xFF0B1220.toInt()// text on the bright focus container
    const val ON_PRIMARY: Int = 0xFFFFFFFF.toInt()        // text on the primary container

    // Content
    const val ON_BACKGROUND: Int = 0xFFE6EDF5.toInt()     // body text
    const val ON_SURFACE: Int = 0xFFE6EDF5.toInt()        // body text on cards
    const val MUTED_TEXT: Int = 0xFF9AA5B4.toInt()        // secondary text

    // Borders
    const val BORDER_UNFOCUSED: Int = 0xFF2A3547.toInt()
    const val BORDER_FOCUSED: Int = 0xFFE6EDF5.toInt()

    /** Every body-text pair the UI draws, for the WCAG AA evidence (M1.1 A4). */
    val CHECKED_PAIRS: List<Triple<String, Int, Int>> = listOf(
        Triple("onBackground on background (body text)", ON_BACKGROUND, BACKGROUND),
        Triple("onSurface on surface (cards, HUD, rows)", ON_SURFACE, SURFACE),
        Triple("mutedText on background (labels, notes)", MUTED_TEXT, BACKGROUND),
        Triple("mutedText on surface (secondary text on cards)", MUTED_TEXT, SURFACE),
        Triple("onFocusContainer on focusContainer (focused rows/buttons)", ON_FOCUS_CONTAINER, FOCUS_CONTAINER),
        Triple("onPrimary on primary (primary-filled states)", ON_PRIMARY, PRIMARY),
        Triple("onSurface on surfaceRaised (button labels, unfocused)", ON_SURFACE, SURFACE_RAISED),
        Triple("mutedText on surfaceRaised (muted button labels)", MUTED_TEXT, SURFACE_RAISED),
    )

    /** WCAG relative luminance of an ARGB int colour. */
    fun relativeLuminance(argb: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two ARGB int colours (1.0 .. 21.0). */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
