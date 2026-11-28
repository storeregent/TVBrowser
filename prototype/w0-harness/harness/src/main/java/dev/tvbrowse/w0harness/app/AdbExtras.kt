package dev.tvbrowse.w0harness.app

/**
 * Pure parser for the adb automation intent extras (M1.1 D-5 + M2 triggers).
 *
 * The launcher EntryActivity receives them via `am start -e/--ez` and forwards
 * them to the browser activity. Parsing is unit-tested; `fromIntent` assembly
 * lives in the activities.
 *
 * | Extra | Meaning |
 * |---|---|
 * | `w0h.url` | Open the URL (same path as Go: normalised, NAV_REQUEST source=adb) |
 * | `w0h.screen` | home \| tabs \| deviceinfo \| logs \| settings \| tests |
 * | `w0h.action` | M1: export_deviceinfo \| export_all \| mark_step (+ `w0h.label`)
 * |              | M2: open_tab \| switch_tab \| crash_active \| terminate_renderer \| set_standby |
 * | `w0h.label` | Label for mark_step |
 * | `w0h.value` | Generic value: switch_tab -> tab id, set_standby -> prewarm\|mru\|none |
 * | `w0h.locked` | "true"/"false": pick the Locked/Unlocked variant for this launch only |
 */
data class AdbLaunchCommands(
    val url: String? = null,
    val screen: Screen? = null,
    val action: AdbAction? = null,
    val label: String? = null,
    val value: String? = null,
    val lockedOverride: Boolean? = null,
    /** Keys whose values were present but could not be parsed (logged by the caller). */
    val ignored: List<String> = emptyList(),
) {
    val hasAny: Boolean
        get() = url != null || screen != null || action != null || lockedOverride != null || ignored.isNotEmpty()
}

enum class AdbAction {
    EXPORT_DEVICEINFO,
    EXPORT_ALL,
    MARK_STEP,
    // M2 triggers (brief §5.3/§5.4)
    OPEN_TAB,
    SWITCH_TAB,
    CRASH_ACTIVE,
    TERMINATE_RENDERER,
    SET_STANDBY,
}

object AdbExtras {

    const val KEY_URL = "w0h.url"
    const val KEY_SCREEN = "w0h.screen"
    const val KEY_ACTION = "w0h.action"
    const val KEY_LABEL = "w0h.label"
    const val KEY_VALUE = "w0h.value"
    const val KEY_LOCKED = "w0h.locked"

    fun parse(extras: Map<String, String?>): AdbLaunchCommands {
        val ignored = mutableListOf<String>()

        val url = extras[KEY_URL]?.trim()?.takeIf { it.isNotEmpty() }

        val screen = extras[KEY_SCREEN]?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { raw ->
            when (raw) {
                "home" -> Screen.Home
                "tabs" -> Screen.Tabs
                "browser" -> Screen.Browser
                "deviceinfo" -> Screen.DeviceInfo
                "logs" -> Screen.Logs
                "settings" -> Screen.Settings
                "tests" -> Screen.Tests
                else -> {
                    ignored.add(KEY_SCREEN)
                    null
                }
            }
        }

        val action = extras[KEY_ACTION]?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { raw ->
            when (raw) {
                "export_deviceinfo" -> AdbAction.EXPORT_DEVICEINFO
                "export_all" -> AdbAction.EXPORT_ALL
                "mark_step" -> AdbAction.MARK_STEP
                "open_tab" -> AdbAction.OPEN_TAB
                "switch_tab" -> AdbAction.SWITCH_TAB
                "crash_active" -> AdbAction.CRASH_ACTIVE
                "terminate_renderer" -> AdbAction.TERMINATE_RENDERER
                "set_standby" -> AdbAction.SET_STANDBY
                else -> {
                    ignored.add(KEY_ACTION)
                    null
                }
            }
        }

        val label = extras[KEY_LABEL]?.trim()?.takeIf { it.isNotEmpty() }

        val value = extras[KEY_VALUE]?.trim()?.takeIf { it.isNotEmpty() }

        val locked = extras[KEY_LOCKED]?.trim()?.lowercase()?.let { raw ->
            when (raw) {
                "true" -> true
                "false" -> false
                else -> {
                    if (raw.isNotEmpty()) ignored.add(KEY_LOCKED)
                    null
                }
            }
        }

        return AdbLaunchCommands(
            url = url,
            screen = screen,
            action = action,
            label = label,
            value = value,
            lockedOverride = locked,
            ignored = ignored,
        )
    }
}
