package dev.tvbrowse.w0harness.app

import android.content.Context

/**
 * Settings backing store. Uses framework SharedPreferences on purpose: DataStore
 * is not in the harness dependency allow-list (brief §1.4).
 *
 * All keys — including the ones whose behaviour ships in M2/M3 — are included in
 * `snapshot()` so the session header records "all current Settings values" (§6).
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- M1 (functional now) ---

    /** Which browser activity variant EntryActivity starts (§3.2). Applies on next launch. */
    fun activityVariant(): String = prefs.getString(KEY_ACTIVITY_VARIANT, VARIANT_LOCKED) ?: VARIANT_LOCKED

    fun setActivityVariant(value: String) = prefs.edit().putString(KEY_ACTIVITY_VARIANT, value).apply()

    /** Per-WebView Safe Browsing default for new WebViews (§3.1). Default: off. */
    fun safeBrowsingEnabled(): Boolean = prefs.getBoolean(KEY_SAFE_BROWSING, false)

    fun setSafeBrowsingEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_SAFE_BROWSING, value).apply()

    /** Desktop vs mobile user agent for new WebViews (§4.7). */
    fun desktopUserAgent(): Boolean = prefs.getBoolean(KEY_DESKTOP_UA, false)

    fun setDesktopUserAgent(value: Boolean) = prefs.edit().putBoolean(KEY_DESKTOP_UA, value).apply()

    // --- Future milestones (recorded in the header, UI shows them disabled until then) ---

    fun standbyMode(): String = prefs.getString(KEY_STANDBY_MODE, "prewarm") ?: "prewarm" // M2 (§5.3)

    /** M2: persists the T-05 standby mode (prewarm | mru | none). */
    fun setStandbyModeValue(value: String) = prefs.edit().putString(KEY_STANDBY_MODE, value).apply()

    fun popupPolicy(): Int = prefs.getInt(KEY_POPUP_POLICY, 1) // M3 (§5.6)

    fun crashGuardWindowSeconds(): Int = prefs.getInt(KEY_CRASH_GUARD_WINDOW, 60) // M2 (§5.4)

    /** M2: persists the crash-guard window (§5.4). Applies to new crash sequences. */
    fun setCrashGuardWindowSeconds(value: Int) = prefs.edit().putInt(KEY_CRASH_GUARD_WINDOW, value).apply()

    fun cursorEnabled(): Boolean = prefs.getBoolean(KEY_CURSOR_ENABLED, false) // M3 (§5.9)

    fun cursorInjectionMode(): String = prefs.getString(KEY_CURSOR_INJECTION, "synthetic_touch") ?: "synthetic_touch" // M3 (§5.9)

    fun testBlocklistEnabled(): Boolean = prefs.getBoolean(KEY_TEST_BLOCKLIST, false) // M3 (§5.7)

    /** All current settings values, for the session header (brief §6). */
    fun snapshot(): Map<String, Any?> = sortedMapOf(
        KEY_ACTIVITY_VARIANT to activityVariant(),
        KEY_SAFE_BROWSING to safeBrowsingEnabled(),
        KEY_DESKTOP_UA to desktopUserAgent(),
        KEY_STANDBY_MODE to standbyMode(),
        KEY_POPUP_POLICY to popupPolicy(),
        KEY_CRASH_GUARD_WINDOW to crashGuardWindowSeconds(),
        KEY_CURSOR_ENABLED to cursorEnabled(),
        KEY_CURSOR_INJECTION to cursorInjectionMode(),
        KEY_TEST_BLOCKLIST to testBlocklistEnabled(),
    )

    companion object {
        const val PREFS_NAME = "w0_settings"

        const val VARIANT_LOCKED = "locked"
        const val VARIANT_UNLOCKED = "unlocked"

        const val KEY_ACTIVITY_VARIANT = "activity_variant"
        const val KEY_SAFE_BROWSING = "safe_browsing_enabled"
        const val KEY_DESKTOP_UA = "desktop_user_agent"
        const val KEY_STANDBY_MODE = "standby_mode"
        const val KEY_POPUP_POLICY = "popup_policy"
        const val KEY_CRASH_GUARD_WINDOW = "crash_guard_window_seconds"
        const val KEY_CURSOR_ENABLED = "cursor_enabled"
        const val KEY_CURSOR_INJECTION = "cursor_injection_mode"
        const val KEY_TEST_BLOCKLIST = "test_blocklist_enabled"
    }
}
