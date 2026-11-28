package dev.tvbrowse.w0harness

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import dev.tvbrowse.w0harness.app.AdbAction
import dev.tvbrowse.w0harness.app.AdbExtras
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.info.DeviceInfoCollector
import dev.tvbrowse.w0harness.log.LogIo
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.ui.HarnessRoot
import dev.tvbrowse.w0harness.util.ConfigurationDiff
import dev.tvbrowse.w0harness.util.UrlNormalizer
import dev.tvbrowse.w0harness.util.jsonObject
import dev.tvbrowse.w0harness.web.StandbyMode
import dev.tvbrowse.w0harness.web.WebViewPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shared browser activity implementation (brief §3.2). The two concrete classes
 * differ only in their manifest `configChanges` attribute:
 *  - [BrowserActivityLocked] (default): FR-14 config-change list, no recreation;
 *  - [BrowserActivityUnlocked]: no configChanges, reproduces the A-01 failure mode.
 *
 * This activity also hosts the M1 lifecycle & key logger (§5.2) and the M1.1
 * additions: BACK_DISPATCH decision logging (D-7), adb hook application (D-5)
 * and the onStop log flush (D-4).
 */
abstract class BrowserActivityBase : ComponentActivity() {

    protected lateinit var pool: WebViewPool
        private set
    protected lateinit var settings: SettingsStore
        private set

    /** Set by [applyAdbLaunchCommands]; suppresses the session-restore offer (§5.5). */
    private var launchHadAdbHooks = false

    protected abstract val activityVariant: String

    private var lastConfigFields: Map<String, String> = emptyMap()
    private var inputManager: InputManager? = null
    private var displayManager: DisplayManager? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        SessionLog.log(
            "ACTIVITY_CREATE",
            jsonObject(
                "variant" to activityVariant,
                "class" to javaClass.simpleName,
                "hasSavedInstanceState" to (savedInstanceState != null),
            ),
        )
        super.onCreate(savedInstanceState)
        settings = SettingsStore(this)
        pool = WebViewPool(this, settings)
        lastConfigFields = configurationFields(resources.configuration)

        onBackPressedDispatcher.addCallback(this, backCallback)
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_CREATE) {
                    SessionLog.log("LIFECYCLE", jsonObject("event" to event.name, "activity" to javaClass.simpleName))
                }
            },
        )
        registerInputAndDisplayListeners()
        setupContent()
        applyAdbLaunchCommands()
        offerRestoreIfAppropriate()
    }

    override fun onDestroy() {
        SessionLog.log(
            "ACTIVITY_DESTROY",
            jsonObject("variant" to activityVariant, "isFinishing" to isFinishing),
        )
        inputManager?.unregisterInputDeviceListener(inputListener)
        displayManager?.unregisterDisplayListener(displayListener)
        pool.destroyAll("activity_destroyed")
        super.onDestroy()
    }

    /** D-4: flush the async log writer when the app goes to the background. */
    override fun onStop() {
        SessionLog.log("ACTIVITY_STOP", jsonObject("variant" to activityVariant))
        super.onStop()
        pool.onActivityStopped() // M2 §5.3: global pauseTimers + CookieManager.flush (C-15)
        LogIo.flushSync()
    }

    override fun onResume() {
        super.onResume()
        pool.onActivityResumed() // M2 §5.3: resumeTimers (global)
    }

    // ------------------------------------------------------------------ fullscreen container (§5.2/AA D-gap-09)

    private lateinit var fullscreenOverlay: FrameLayout
    private var fullscreenCallback: android.webkit.WebChromeClient.CustomViewCallback? = null

    private val isFullscreenShowing: Boolean
        get() = ::fullscreenOverlay.isInitialized && fullscreenOverlay.visibility == View.VISIBLE

    /**
     * The activity root is a plain FrameLayout: the ComposeView plus a
     * full-window overlay container for WebView custom views (video/element
     * fullscreen) OUTSIDE the Compose tree (AA D-gap-09).
     */
    private fun setupContent() {
        val root = FrameLayout(this)
        val compose = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent { HarnessRoot(activity = this@BrowserActivityBase, pool = pool, settings = settings, onNavigateBack = ::navigateBack) }
        }
        root.addView(compose, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        fullscreenOverlay = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(android.graphics.Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        root.addView(fullscreenOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)
        pool.fullscreenHost = object : WebViewPool.FullscreenHost {
            override fun enterFullscreen(view: View, callback: android.webkit.WebChromeClient.CustomViewCallback) {
                fullscreenCallback = callback
                fullscreenOverlay.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                fullscreenOverlay.visibility = View.VISIBLE
                fullscreenOverlay.requestFocus()
            }

            override fun exitFullscreen(): Boolean {
                if (!isFullscreenShowing) return false
                fullscreenOverlay.removeAllViews()
                fullscreenOverlay.visibility = View.GONE
                runCatching { fullscreenCallback?.onCustomViewHidden() }
                fullscreenCallback = null
                return true
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newFields = configurationFields(newConfig)
        val changed = ConfigurationDiff.changedFields(lastConfigFields, newFields)
        SessionLog.log(
            "CONFIGURATION_CHANGED",
            jsonObject(
                "variant" to activityVariant,
                "changed" to jsonObject(changed.map { (k, v) -> k to jsonObject("from" to v.first, "to" to v.second) }),
            ),
        )
        lastConfigFields = newFields
    }

    /**
     * D-5: the browser activity is `singleTask`, so an `am start` on the launcher
     * while the app is alive arrives here as `onNewIntent` (forwarded by
     * EntryActivity). Re-apply the adb launch hooks against the new extras.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SessionLog.log("ACTIVITY_NEW_INTENT", jsonObject("variant" to activityVariant))
        applyAdbLaunchCommands()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        SessionLog.log("WINDOW_FOCUS_CHANGED", jsonObject("hasFocus" to hasFocus))
        super.onWindowFocusChanged(hasFocus)
    }

    // ------------------------------------------------------------------ back key (D-7)

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            AppState.backCallbackInvocations++
            val canGoBack = pool.currentWebView?.canGoBack()
            when {
                isFullscreenShowing -> {
                    // §5.2 M2: Back exits fullscreen video first (D-gap-09).
                    logBackDispatch(canGoBack, "fullscreen_exit")
                    pool.fullscreenHost?.exitFullscreen()
                }
                AppState.screen == Screen.Browser && canGoBack == true -> {
                    logBackDispatch(canGoBack, "webview_history")
                    pool.handleBack()
                }
                AppState.screen != Screen.Home -> {
                    logBackDispatch(canGoBack, "screen_navigation")
                    AppState.screen = Screen.Home
                }
                else -> {
                    // Platform default (HARNESS_BRIEF does not override it): Back at
                    // Home with no web history and no open dialog finishes the activity.
                    logBackDispatch(canGoBack, "activity_finish")
                    finish()
                }
            }
        }
    }

    private fun logBackDispatch(canGoBack: Boolean?, handledBy: String) {
        SessionLog.log(
            "BACK_DISPATCH",
            jsonObject(
                "screen" to AppState.screen.name,
                "canGoBack" to canGoBack,
                "handledBy" to handledBy,
                "variant" to activityVariant,
                "callbackInvocations" to AppState.backCallbackInvocations,
            ),
        )
    }

    /** Shared back semantics kept for the HUD Back button. */
    private fun navigateBack() {
        when {
            isFullscreenShowing -> pool.fullscreenHost?.exitFullscreen()
            AppState.screen == Screen.Browser && pool.handleBack() -> Unit
            AppState.screen != Screen.Home -> AppState.screen = Screen.Home
            else -> finish() // clean exit clears the session_open marker
        }
    }

    // ------------------------------------------------------------------ adb launch hooks (D-5)

    private fun applyAdbLaunchCommands() {
        launchHadAdbHooks = false
        val extras: Map<String, String?> = mapOf(
            AdbExtras.KEY_URL to intent.getStringExtra(AdbExtras.KEY_URL),
            AdbExtras.KEY_SCREEN to intent.getStringExtra(AdbExtras.KEY_SCREEN),
            AdbExtras.KEY_ACTION to intent.getStringExtra(AdbExtras.KEY_ACTION),
            AdbExtras.KEY_LABEL to intent.getStringExtra(AdbExtras.KEY_LABEL),
            AdbExtras.KEY_VALUE to intent.getStringExtra(AdbExtras.KEY_VALUE),
            AdbExtras.KEY_LOCKED to (intent.getStringExtra(AdbExtras.KEY_LOCKED)
                ?: intent.extras?.get(AdbExtras.KEY_LOCKED)?.toString()),
        )
        val adb = AdbExtras.parse(extras)
        if (!adb.hasAny) return
        launchHadAdbHooks = true
        SessionLog.log(
            "ADB_HOOKS",
            jsonObject(
                "url" to adb.url,
                "screen" to adb.screen?.name,
                "action" to adb.action?.name,
                "label" to adb.label,
                "ignored" to adb.ignored,
                "stage" to "browser",
            ),
        )

        adb.screen?.let { screen ->
            AppState.screen = screen
            SessionLog.log("ADB_SCREEN_OPENED", jsonObject("screen" to screen.name))
        }

        adb.url?.let { raw ->
            val normalized = UrlNormalizer.normalize(raw)
            if (normalized == null) {
                SessionLog.log(
                    "NAV_REQUEST_IGNORED",
                    jsonObject("source" to "adb", "input" to raw, "reason" to "does not parse as a URL"),
                )
            } else {
                SessionLog.log(
                    "NAV_REQUEST",
                    jsonObject("source" to "adb", "requested" to raw, "normalized" to normalized),
                )
                AppState.pendingUrl = normalized
                AppState.screen = Screen.Browser
            }
        }

        adb.action?.let { action ->
            when (action) {
                AdbAction.MARK_STEP -> {
                    val label = adb.label ?: "(adb mark)"
                    SessionLog.mark(label)
                    AppState.actionResult = "Marked step (adb): $label"
                    if (AppState.screen == Screen.Browser) AppState.screen = Screen.Home
                }
                AdbAction.EXPORT_DEVICEINFO -> {
                    AppState.screen = Screen.DeviceInfo
                    performExportDeviceinfo()
                }
                AdbAction.EXPORT_ALL -> {
                    AppState.screen = Screen.Logs
                    performExportAll()
                }
                // ---------------------------------------------------------------- M2 triggers (§5.3/§5.4)
                AdbAction.OPEN_TAB -> {
                    val normalized = adb.url?.let { UrlNormalizer.normalize(it) }
                    if (adb.url != null && normalized == null) {
                        SessionLog.log("ADB_HOOKS_IGNORED", jsonObject("key" to AdbExtras.KEY_URL, "reason" to "does not parse as a URL"))
                    }
                    pool.openTab(normalized)
                    AppState.screen = Screen.Browser
                    SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "open_tab", "tabs" to pool.manager.tabCount))
                }
                AdbAction.SWITCH_TAB -> {
                    val id = adb.value?.toIntOrNull()
                    if (id != null && pool.switchTo(id, trigger = "adb")) {
                        AppState.screen = Screen.Browser
                        SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "switch_tab", "tabId" to id))
                    } else {
                        SessionLog.log("ADB_HOOKS_IGNORED", jsonObject("key" to AdbExtras.KEY_VALUE, "reason" to "switch_tab needs a valid tab id"))
                    }
                }
                AdbAction.CRASH_ACTIVE -> {
                    AppState.screen = Screen.Browser
                    pool.triggerCrashInActive()
                    SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "crash_active"))
                }
                AdbAction.TERMINATE_RENDERER -> {
                    AppState.screen = Screen.Browser
                    pool.terminateActiveRenderer()
                    SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "terminate_renderer"))
                }
                AdbAction.SET_STANDBY -> {
                    val mode = StandbyMode.fromId(adb.value)
                    if (mode != null) {
                        pool.setStandbyMode(mode, trigger = "adb")
                        SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "set_standby", "mode" to mode.id))
                    } else {
                        SessionLog.log(
                            "ADB_HOOKS_IGNORED",
                            jsonObject("key" to AdbExtras.KEY_VALUE, "reason" to "set_standby needs prewarm|mru|none"),
                        )
                    }
                }
            }
        }
    }

    /**
     * §5.5: on a cold start with persisted tab state (and an unclean previous
     * exit, and no adb hooks that steer this launch), offer "Restore session".
     */
    private fun offerRestoreIfAppropriate() {
        pool.offerRestoreAsync { !launchHadAdbHooks && AppState.previousSessionDied }
    }

    private fun performExportDeviceinfo() {
        val activity = this
        lifecycleScope.launch(Dispatchers.IO) {
            val file = SessionLog.exportDeviceInfo(DeviceInfoCollector.collect(activity))
            SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "export_deviceinfo", "path" to file.absolutePath))
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                AppState.actionResult = "Exported: ${file.absolutePath}"
            }
        }
    }

    private fun performExportAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            val zip = SessionLog.exportAll()
            SessionLog.log("ADB_ACTION_RESULT", jsonObject("action" to "export_all", "path" to zip.absolutePath))
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                AppState.actionResult = "Exported: ${zip.absolutePath}"
            }
        }
    }

    // ------------------------------------------------------------------ key events (§5.2, T-07)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        logKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK) {
            AppState.backKeyEventsSeen++
        }

        // HUD toggle: dedicated Menu key (§4.2).
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) {
            toggleHud("menu_key")
            return true
        }

        // HUD toggle: long-press OK while the page (not the HUD) has focus (§4.2).
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER &&
            event.action == KeyEvent.ACTION_UP &&
            !AppState.hudVisible &&
            (event.eventTime - event.downTime) >= LONG_OK_MS
        ) {
            toggleHud("long_ok")
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun logKeyEvent(event: KeyEvent) {
        SessionLog.log(
            "KEY_EVENT",
            jsonObject(
                "code" to KeyEvent.keyCodeToString(event.keyCode),
                "keyCode" to event.keyCode,
                "action" to actionName(event.action),
                "repeat" to event.repeatCount,
                "deviceId" to event.deviceId,
                "source" to event.source,
                "longPress" to ((event.flags and KeyEvent.FLAG_LONG_PRESS) != 0),
            ),
        )
    }

    private fun actionName(action: Int): String = when (action) {
        KeyEvent.ACTION_DOWN -> "DOWN"
        KeyEvent.ACTION_UP -> "UP"
        KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
        else -> action.toString()
    }

    private fun toggleHud(trigger: String) {
        AppState.hudVisible = !AppState.hudVisible
        AppState.hudToggles++
        SessionLog.log(
            "HUD_TOGGLED",
            jsonObject("visible" to AppState.hudVisible, "trigger" to trigger, "count" to AppState.hudToggles),
        )
    }

    // ------------------------------------------------------------------ input / display listeners (§5.2)

    private val inputListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            SessionLog.log("INPUT_DEVICE_ADDED", jsonObject("deviceId" to deviceId, "device" to describeDevice(deviceId)))
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            SessionLog.log("INPUT_DEVICE_REMOVED", jsonObject("deviceId" to deviceId))
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            SessionLog.log("INPUT_DEVICE_CHANGED", jsonObject("deviceId" to deviceId, "device" to describeDevice(deviceId)))
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            SessionLog.log("DISPLAY_ADDED", jsonObject("displayId" to displayId))
        }

        override fun onDisplayRemoved(displayId: Int) {
            SessionLog.log("DISPLAY_REMOVED", jsonObject("displayId" to displayId))
        }

        override fun onDisplayChanged(displayId: Int) {
            SessionLog.log("DISPLAY_CHANGED", jsonObject("displayId" to displayId))
        }
    }

    private fun registerInputAndDisplayListeners() {
        inputManager = (getSystemService(Context.INPUT_SERVICE) as InputManager).also {
            it.registerInputDeviceListener(inputListener, mainHandler)
        }
        displayManager = (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).also {
            it.registerDisplayListener(displayListener, mainHandler)
        }
    }

    private fun describeDevice(deviceId: Int): Map<String, Any?> {
        val device: InputDevice? = InputDevice.getDevice(deviceId)
        if (device == null) return mapOf<String, Any?>("name" to null)
        return mapOf(
            "name" to device.name,
            "sources" to describeSources(device.sources),
            "vendorId" to device.vendorId,
            "productId" to device.productId,
            "isExternal" to if (Build.VERSION.SDK_INT >= 29) device.isExternal else null,
        )
    }

    private fun describeSources(sources: Int): List<String> {
        val names = mutableListOf<String>()
        var remaining = sources
        var bit = 1
        while (remaining != 0 && bit <= (1 shl 26)) {
            if (remaining and bit != 0) names.add("0x%x".format(bit))
            remaining = remaining and bit.inv()
            bit = bit shl 1
        }
        return names
    }

    companion object {
        private const val LONG_OK_MS = 600L

        /** Extracts the Configuration fields we diff in CONFIGURATION_CHANGED (§5.2). */
        fun configurationFields(c: Configuration): Map<String, String> = sortedMapOf(
            "fontScale" to c.fontScale.toString(),
            "hardKeyboardHidden" to c.hardKeyboardHidden.toString(),
            "keyboard" to c.keyboard.toString(),
            "keyboardHidden" to c.keyboardHidden.toString(),
            "navigation" to c.navigation.toString(),
            "navigationHidden" to c.navigationHidden.toString(),
            "orientation" to c.orientation.toString(),
            "screenLayout" to c.screenLayout.toString(),
            "smallestScreenWidthDp" to c.smallestScreenWidthDp.toString(),
            "screenWidthDp" to c.screenWidthDp.toString(),
            "screenHeightDp" to c.screenHeightDp.toString(),
            "densityDpi" to c.densityDpi.toString(),
            "touchscreen" to c.touchscreen.toString(),
            "uiMode" to c.uiMode.toString(),
        )
    }
}
