package dev.tvbrowse.w0harness.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** CONFIGURATION_CHANGED logging (§5.2) must report exactly the changed fields. */
class ConfigurationDiffTest {

    @Test
    fun `identical maps produce no diff`() {
        val m = mapOf("uiMode" to "1", "densityDpi" to "320")
        assertTrue(ConfigurationDiff.changedFields(m, m).isEmpty())
    }

    @Test
    fun `only changed fields are reported`() {
        val old = mapOf("uiMode" to "1", "densityDpi" to "320", "keyboardHidden" to "1")
        val new = mapOf("uiMode" to "1", "densityDpi" to "160", "keyboardHidden" to "2")
        val diff = ConfigurationDiff.changedFields(old, new)
        assertEquals(setOf("densityDpi", "keyboardHidden"), diff.keys)
        assertEquals("320" to "160", diff["densityDpi"])
        assertEquals("1" to "2", diff["keyboardHidden"])
    }

    @Test
    fun `missing keys are reported as absent`() {
        val diff = ConfigurationDiff.changedFields(mapOf("navigation" to "1"), emptyMap())
        assertEquals("1" to "<absent>", diff["navigation"])
    }
}
