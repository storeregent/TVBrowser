package dev.tvbrowse.w0harness

import dev.tvbrowse.w0harness.app.SettingsStore

/**
 * Default browser activity. Its manifest entry swallows the FR-14
 * config-change list, so BT-remote/mouse/orientation changes do NOT recreate
 * the activity (brief §3.2). Used unless Settings selects the unlocked variant.
 */
class BrowserActivityLocked : BrowserActivityBase() {
    override val activityVariant: String = SettingsStore.VARIANT_LOCKED
}
