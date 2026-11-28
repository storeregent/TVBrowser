package dev.tvbrowse.w0harness

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.Screen
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.ui.HarnessRoot
import dev.tvbrowse.w0harness.util.ConfigurationDiff
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Shared browser activity implementation (brief §3.2). The two concrete classes
 * differ only in their manifest `configChanges` attribute:
 *  - [BrowserActivityLocked] (default): FR-14 config-change list, no recreation;
 *  - [BrowserActivityUnlocked]: no configChanges, reproduces the A-01 failure mode.
 *
 * This activity also hosts the M1 lifecycle & key logger (§5.2): every lifecycle
 * callback, every key event, configuration-change diffs, input/display device
 * events and the T-07 back-key counters go through [SessionLog].
 */
abstract class BrowserActivityBase : ComponentActivity() {

    protected lateinit var pool: WebViewPool
        private set
    protected lateinit var settings: SettingsStore
        private set

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
        setContent {
            HarnessRoot(activity = this, pool = pool, settings = settings, onNavigateBack = ::navigateBack)
        }
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        SessionLog.log("WINDOW_FOCUS_CHANGED", jsonObject("hasFocus" to hasFocus))
        super.onWindowFocusChanged(hasFocus)
    }

    // ------------------------------------------------------------------ back key

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            AppState.backCallbackInvocations++
            SessionLog.log(
                "BACK_CALLBACK_INVOKED",
                jsonObject("count" to AppState.backCallbackInvocations, "screen" to AppState.screen.name),
            )
            navigateBack()
        }
    }

    /** Shared back semantics: WebView history first, then screen stack, then finish. */
    private fun navigateBack() {
        when {
            AppState.screen == Screen.Browser && pool.handleBack() -> Unit
            AppState.screen != Screen.Home -> AppState.screen = Screen.Home
            else -> finish() // clean exit clears the session_open marker
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
                "longPress" to (event.flags and KeyEvent.FLAG_LONG_PRESS) != 0,
            ),
        )
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
