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
    fun `w0h screen accepts all seven names case-insensitively`() {
        assertEquals(Screen.Home, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "home")).screen)
        assertEquals(Screen.Browser, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "Browser")).screen)
        assertEquals(Screen.Tabs, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "tabs")).screen)
        assertEquals(Screen.DeviceInfo, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "DeviceInfo")).screen)
        assertEquals(Screen.Logs, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "LOGS")).screen)
        assertEquals(Screen.Settings, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "settings")).screen)
        assertEquals(Screen.Tests, AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "Tests")).screen)
    }

    @Test
    fun `unknown screen value is reported as ignored`() {
        val parsed = AdbExtras.parse(mapOf(AdbExtras.KEY_SCREEN to "nowhere"))
        assertNull(parsed.screen)
        assertTrue(parsed.hasAny)
        assertEquals(listOf(AdbExtras.KEY_SCREEN), parsed.ignored)
    }

    @Test
    fun `w0h action accepts the M1 actions`() {
        assertEquals(
            AdbAction.EXPORT_DEVICEINFO,
            AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "export_deviceinfo")).action,
        )
        assertEquals(AdbAction.EXPORT_ALL, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "EXPORT_ALL")).action)
        assertEquals(AdbAction.MARK_STEP, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "mark_step")).action)
    }

    @Test
    fun `w0h action accepts the M2 triggers`() {
        assertEquals(AdbAction.OPEN_TAB, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "open_tab")).action)
        assertEquals(AdbAction.SWITCH_TAB, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "SWITCH_TAB")).action)
        assertEquals(AdbAction.CRASH_ACTIVE, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "crash_active")).action)
        assertEquals(AdbAction.TERMINATE_RENDERER, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "terminate_renderer")).action)
        assertEquals(AdbAction.SET_STANDBY, AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "set_standby")).action)
    }

    @Test
    fun `w0h value carries switch and standby arguments`() {
        val switch = AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "switch_tab", AdbExtras.KEY_VALUE to "2"))
        assertEquals(AdbAction.SWITCH_TAB, switch.action)
        assertEquals("2", switch.value)
        val standby = AdbExtras.parse(mapOf(AdbExtras.KEY_ACTION to "set_standby", AdbExtras.KEY_VALUE to "mru"))
        assertEquals(AdbAction.SET_STANDBY, standby.action)
        assertEquals("mru", standby.value)
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
