package dev.tvbrowse.w0harness.app

/**
 * Pure parser for the M1.1 D-5 adb automation intent extras.
 *
 * The launcher EntryActivity receives them via `am start -e/--ez` and forwards
 * them to the browser activity. Parsing is unit-tested (A8); `fromIntent` is a
 * thin Android-only wrapper.
 *
 * | Extra | Meaning |
 * |---|---|
 * | `w0h.url` | Open the URL (same path as Go: normalised, NAV_REQUEST source=adb) |
 * | `w0h.screen` | home \| deviceinfo \| logs \| settings \| tests |
 * | `w0h.action` | export_deviceinfo \| export_all \| mark_step (+ `w0h.label`) |
 * | `w0h.locked` | "true"/"false": pick the Locked/Unlocked variant for this launch only |
 */
data class AdbLaunchCommands(
    val url: String? = null,
    val screen: Screen? = null,
    val action: AdbAction? = null,
    val label: String? = null,
    val lockedOverride: Boolean? = null,
    /** Keys whose values were present but could not be parsed (logged by the caller). */
    val ignored: List<String> = emptyList(),
) {
    val hasAny: Boolean
        get() = url != null || screen != null || action != null || lockedOverride != null || ignored.isNotEmpty()
}

enum class AdbAction { EXPORT_DEVICEINFO, EXPORT_ALL, MARK_STEP }

object AdbExtras {

    const val KEY_URL = "w0h.url"
    const val KEY_SCREEN = "w0h.screen"
    const val KEY_ACTION = "w0h.action"
    const val KEY_LABEL = "w0h.label"
    const val KEY_LOCKED = "w0h.locked"

    fun parse(extras: Map<String, String?>): AdbLaunchCommands {
        val ignored = mutableListOf<String>()

        val url = extras[KEY_URL]?.trim()?.takeIf { it.isNotEmpty() }

        val screen = extras[KEY_SCREEN]?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { raw ->
            when (raw) {
                "home" -> Screen.Home
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
                else -> {
                    ignored.add(KEY_ACTION)
                    null
                }
            }
        }

        val label = extras[KEY_LABEL]?.trim()?.takeIf { it.isNotEmpty() }

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
            lockedOverride = locked,
            ignored = ignored,
        )
    }
}
