package dev.tvbrowse.w0harness

import dev.tvbrowse.w0harness.app.SettingsStore

/**
 * Unlocked browser activity: its manifest entry has NO configChanges, so it
 * recreates on BT-remote/mouse config changes — reproducing the A-01 failure
 * mode for P1 (brief §3.2). Same UI code as [BrowserActivityLocked].
 */
class BrowserActivityUnlocked : BrowserActivityBase() {
    override val activityVariant: String = SettingsStore.VARIANT_UNLOCKED
}
