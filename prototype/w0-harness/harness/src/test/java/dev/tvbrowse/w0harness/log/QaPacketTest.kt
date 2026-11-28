package dev.tvbrowse.w0harness.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session header must carry the full QA packet (brief §6): device model +
 * fingerprint, OS version, RAM class, WebView package + version, harness
 * version/flavor/git SHA/activity variant, and all current Settings values.
 */
class QaPacketTest {

    private fun packet(settings: Map<String, Any?>) = QaPacket.build(
        deviceModel = "Google TV Stick",
        deviceFingerprint = "google/amati/amati:14/UTT3.240/master",
        osRelease = "14",
        osSdkInt = 34,
        osSecurityPatch = "2026-11-01",
        isLowRamDevice = false,
        totalMemMb = 2048,
        memoryClassMb = 128,
        largeMemoryClassMb = 256,
        webViewPackageName = "com.google.android.webview",
        webViewVersionName = "140.0.0.0",
        webViewVersionCode = null,
        harnessVersion = "0.1.0-m1",
        flavor = "standard",
        gitSha = "abc1234",
        buildTime = "2026-11-28T00:00:00Z",
        activityVariant = "locked",
        settings = settings,
    )

    @Test
    fun `header contains every required section`() {
        val header = packet(mapOf("safe_browsing_enabled" to false))
        val keys = header.keys
        assertTrue(keys.containsAll(setOf("device", "os", "ramClass", "webView", "harness", "settings")))
        assertTrue(header["device"]!!.toString().contains("fingerprint"))
        assertTrue(header["webView"]!!.toString().contains("com.google.android.webview"))
        assertTrue(header["harness"]!!.toString().contains("gitSha"))
        assertTrue(header["harness"]!!.toString().contains("activityVariant"))
    }

    @Test
    fun `all settings values are included`() {
        val header = packet(
            mapOf(
                "activity_variant" to "unlocked",
                "safe_browsing_enabled" to true,
                "desktop_user_agent" to false,
            ),
        )
        val settings = header["settings"].toString()
        assertTrue(settings.contains("activity_variant"))
        assertTrue(settings.contains("safe_browsing_enabled"))
        assertTrue(settings.contains("desktop_user_agent"))
    }

    @Test
    fun `nullable webview version code is tolerated`() {
        assertEquals("null", packet(emptyMap())["webView"].toString().substringAfter("versionCode=").substringBefore(","))
    }
}
