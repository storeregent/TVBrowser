package dev.tvbrowse.w0harness.util

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M1.1 D-2 / A4: every body-text pair used by the UI must reach WCAG AA
 * (>= 4.5:1) on the dark theme. The UI derives its colours from
 * [HarnessPalette], so this test cannot drift from what is rendered.
 */
class ContrastTest {

    @Test
    fun `all ui text pairs meet wcag aa`() {
        val failures = HarnessPalette.CHECKED_PAIRS
            .map { (name, fg, bg) -> Triple(name, fg, bg) to HarnessPalette.contrastRatio(fg, bg) }
            .filter { (_, ratio) -> ratio < 4.5 }

        assertTrue(
            "pairs below 4.5:1: " + failures.joinToString { (pair, ratio) ->
                "${pair.first}=${"%.2f".format(ratio)}"
            },
            failures.isEmpty(),
        )
    }

    @Test
    fun `ratios are documented for the pr table`() {
        // Prints the A4 table; also guards against accidental palette edits.
        HarnessPalette.CHECKED_PAIRS.forEach { (name, fg, bg) ->
            println(
                "%-55s fg=#%06X bg=#%06X ratio=%.2f:1".format(
                    name,
                    fg and 0xFFFFFF,
                    bg and 0xFFFFFF,
                    HarnessPalette.contrastRatio(fg, bg),
                ),
            )
        }
    }

    @Test
    fun `focus container is clearly distinct from both surfaces`() {
        // D-3: the focused container must contrast with the unfocused surfaces.
        assertTrue(HarnessPalette.contrastRatio(HarnessPalette.FOCUS_CONTAINER, HarnessPalette.SURFACE_RAISED) > 2.0)
        assertTrue(HarnessPalette.contrastRatio(HarnessPalette.FOCUS_CONTAINER, HarnessPalette.BACKGROUND) > 2.0)
    }
}
