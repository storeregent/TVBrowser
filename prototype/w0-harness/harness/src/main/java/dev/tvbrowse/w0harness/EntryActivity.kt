package dev.tvbrowse.w0harness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.log.SessionLog

/**
 * Launcher activity: routes to the configured browser activity variant (§3.2)
 * and finishes immediately. The active choice is written into the session log
 * both here (configured) and by the started activity (actual).
 */
class EntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(this)
        val variant = store.activityVariant()
        val target = if (variant == SettingsStore.VARIANT_UNLOCKED) {
            BrowserActivityUnlocked::class.java
        } else {
            BrowserActivityLocked::class.java
        }
        SessionLog.log("ENTRY_ROUTE", jsonObject("configuredVariant" to variant, "target" to target.simpleName))
        startActivity(Intent(this, target))
        finish()
    }
}
