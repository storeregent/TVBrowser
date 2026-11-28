package dev.tvbrowse.w0harness.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M1.1 D-5 / A8: intent-extra parsing for the adb automation hooks. */
class AdbExtrasTest {

    @Test
    fun `empty extras produce no commands`() {
        val parsed = AdbExtras.parse(emptyMap())
        assertFalse(parsed.hasAny)
        assertNull(parsed.url)
        assertNull(parsed.screen)
        assertNull(parsed.action)
        assertNull(parsed.lockedOverride)
    }

    @Test
    fun `w0h url is parsed`() {
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_URL to "https://example.com/"))
        assertTrue(parsed.hasAny)
        assertEquals("https://example.com/", parsed.url)
    }

    @Test
    fun `blank url is treated as absent`() {
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_URL to "   "))
        assertFalse(parsed.hasAny)
    }

    @Test
    fun `w0h screen accepts all five names case-insensitively`() {
        assertEquals(Screen.Home, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "home")).screen)
        assertEquals(Screen.DeviceInfo, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "DeviceInfo")).screen)
        assertEquals(Screen.Logs, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "LOGS")).screen)
        assertEquals(Screen.Settings, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "settings")).screen)
        assertEquals(Screen.Tests, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "Tests")).screen)
    }

    @Test
    fun `unknown screen value is reported as ignored`() {
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "browser"))
        assertNull(parsed.screen)
        assertTrue(parsed.hasAny)
        assertEquals(listOf(AdbExtras.KEY_SCREEN), parsed.ignored)
    }

    @Test
    fun `w0h action accepts the three actions`() {
        assertEquals(
            AdbAction.EXPORT_DEVICEINFO,
            AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "export_deviceinfo")).action,
        )
        assertEquals(AdbAction.EXPORT_ALL, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "EXPORT_ALL")).action)
        assertEquals(AdbAction.MARK_STEP, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "mark_step")).action)
    }

    @Test
    fun `mark_step carries its label`() {
        val parsed = AdbExtras.parse(
            mapOf(
                AdbExtras.KEY_ACTION to "mark_step",
                AdbExtras.KEY_LABEL to "P1: remote asleep",
            ),
        )
        assertEquals(AdbAction.MARK_STEP, parsed.action)
        assertEquals("P1: remote asleep", parsed.label)
    }

    @Test
    fun `unknown action is reported as ignored`() {
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "clear_data"))
        assertNull(parsed.action)
        assertEquals(listOf(AdbExtras.KEY_ACTION), parsed.ignored)
    }

    @Test
    fun `w0h locked parses both boolean spellings`() {
        assertEquals(true, AdbExtras.parse(mapOf(AdbExtras.KEY_LOCKED to "true")).lockedOverride)
        assertEquals(false, AdbExtras.parse(mapOf(AdbExtras.KEY_LOCKED to "False")).lockedOverride)
    }

    @Test
    fun `w0h locked accepts the toString of a Boolean extra`() {
        // `--ez w0h.locked false` arrives as a Boolean extra; the activity converts
        // it with toString() before parsing.
        val booleanExtra = java.lang.Boolean.FALSE
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_LOCKED to booleanExtra.toString()))
        assertEquals(false, parsed.lockedOverride)
    }

    @Test
    fun `invalid locked value is ignored`() {
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_LOCKED to "yes"))
        assertNull(parsed.lockedOverride)
        assertEquals(listOf(AdbExtras.KEY_LOCKED), parsed.ignored)
    }

    @Test
    fun `full adb launch parses url screen action and variant together`() {
        val parsed = AdbExtras.parse(
            mapOf(
                AdbExtras.KEY_URL to "example.com",
                AdbExtras.KEY_SCREEN to "deviceinfo",
                AdbExtras.KEY_ACTION to "export_deviceinfo",
                AdbExtras.KEY_LABEL to "test",
                AdbExtras.KEY_LOCKED to "false",
            ),
        )
        assertEquals("example.com", parsed.url)
        assertEquals(Screen.DeviceInfo, parsed.screen)
        assertEquals(AdbAction.EXPORT_DEVICEINFO, parsed.action)
        assertEquals("test", parsed.label)
        assertEquals(false, parsed.lockedOverride)
        assertTrue(parsed.ignored.isEmpty())
    }
}
