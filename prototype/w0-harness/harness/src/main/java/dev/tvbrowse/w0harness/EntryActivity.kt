package dev.tvbrowse.w0harness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.tvbrowse.w0harness.app.AdbExtras
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Launcher activity: routes to the configured browser activity variant (§3.2)
 * and finishes immediately. The active choice is written into the session log
 * both here (configured) and by the started activity (actual).
 *
 * M1.1 D-5: adb automation extras (`w0h.*`) are parsed here, logged, and
 * forwarded to the browser activity, which applies them after setup. The only
 * exported component is this launcher activity; the browser activities stay
 * `exported="false"`, so the hooks are reachable only via `am start` on the
 * launcher intent (or the app UI itself).
 */
class EntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(this)
        val adb = AdbExtras.parse(
            mapOf(
                AdbExtras.KEY_URL to intent.getStringExtra(AdbExtras.KEY_URL),
                AdbExtras.KEY_SCREEN to intent.getStringExtra(AdbExtras.KEY_SCREEN),
                AdbExtras.KEY_ACTION to intent.getStringExtra(AdbExtras.KEY_ACTION),
                AdbExtras.KEY_LABEL to intent.getStringExtra(AdbExtras.KEY_LABEL),
                // `--ez w0h.locked` delivers a Boolean extra; `-e w0h.locked` a String. Accept both.
                AdbExtras.KEY_LOCKED to intent.getStringExtra(AdbExtras.KEY_LOCKED)
                    ?: intent.extras?.get(AdbExtras.KEY_LOCKED)?.toString(),
            ),
        )
        if (adb.hasAny) {
            SessionLog.log(
                "ADB_HOOKS",
                jsonObject(
                    "url" to adb.url,
                    "screen" to adb.screen?.name,
                    "action" to adb.action?.name,
                    "label" to adb.label,
                    "lockedOverride" to adb.lockedOverride,
                    "ignored" to adb.ignored,
                    "stage" to "entry",
                ),
            )
        }

        val variant = when (adb.lockedOverride) {
            true -> SettingsStore.VARIANT_LOCKED
            false -> SettingsStore.VARIANT_UNLOCKED
            null -> store.activityVariant()
        }
        val target = if (variant == SettingsStore.VARIANT_UNLOCKED) {
            BrowserActivityUnlocked::class.java
        } else {
            BrowserActivityLocked::class.java
        }
        SessionLog.log(
            "ENTRY_ROUTE",
            jsonObject(
                "configuredVariant" to store.activityVariant(),
                "effectiveVariant" to variant,
                "target" to target.simpleName,
                "adbHooks" to adb.hasAny,
            ),
        )
        val forward = Intent(this, target)
        intent.extras?.let { forward.putExtras(it) }
        startActivity(forward)
        finish()
    }
}
