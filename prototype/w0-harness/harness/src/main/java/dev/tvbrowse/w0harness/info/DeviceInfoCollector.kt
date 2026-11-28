package dev.tvbrowse.w0harness.info

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.view.Display
import android.view.InputDevice
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import dev.tvbrowse.w0harness.BuildConfig
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Device Info collector (brief §5.1, T-01/T-02). All values are read locally;
 * nothing here touches the network.
 */
object DeviceInfoCollector {

    /** Every WebViewFeature constant this harness references, plus the full §5.1 matrix list. */
    val FEATURE_MATRIX: List<String> = listOf(
        "MULTI_PROCESS",
        "DOCUMENT_START_SCRIPT",
        "SERVICE_WORKER_BASIC_USAGE",
        "SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST",
        "WEB_MESSAGE_LISTENER",
        "SAFE_BROWSING_ENABLE",
        "START_SAFE_BROWSING",
        "GET_WEB_VIEW_RENDERER",
        "WEB_VIEW_RENDERER_TERMINATE",
        "WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE",
        "MULTI_PROFILE",
        "USER_AGENT_METADATA",
        "REQUESTED_WITH_HEADER_ALLOW_LIST",
        "SAVE_STATE", // saveState-with-size-limit feature (androidx.webkit >= 1.14)
    )

    fun collect(activity: Activity): kotlinx.serialization.json.JsonObject {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val wvPackage = runCatching { WebViewCompat.getCurrentWebViewPackage(activity) }.getOrNull()

        return jsonObject(
            "collectedAt" to System.currentTimeMillis(),
            "device" to jsonObject(
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "device" to Build.DEVICE,
                "fingerprint" to Build.FINGERPRINT,
            ),
            "os" to jsonObject(
                "sdkInt" to Build.VERSION.SDK_INT,
                "release" to Build.VERSION.RELEASE,
                "securityPatch" to Build.VERSION.SECURITY_PATCH,
            ),
            "abis" to jsonObject(
                "supportedAbis" to Build.SUPPORTED_ABIS.toList(),
                "supported64BitAbis" to Build.SUPPORTED_64_BIT_ABIS.toList(),
                "supported32BitAbis" to Build.SUPPORTED_32_BIT_ABIS.toList(),
            ),
            "memory" to jsonObject(
                "isLowRamDevice" to am.isLowRamDevice(),
                "memoryClassMb" to am.memoryClass,
                "largeMemoryClassMb" to am.largeMemoryClass,
                "totalMemMb" to memInfo.totalMem / (1024L * 1024L),
                "availMemMb" to memInfo.availMem / (1024L * 1024L),
                "thresholdMb" to memInfo.threshold / (1024L * 1024L),
            ),
            "webviewProvider" to jsonObject(
                "packageName" to wvPackage?.packageName,
                "versionName" to wvPackage?.versionName,
                "versionCode" to versionCodeOf(wvPackage),
                "isGetWebViewPackageNull" to (wvPackage == null),
                "multiProcessEnabled" to multiProcessState(wvPackage != null),
                "rendererProcesses" to rendererProcesses(activity),
            ),
            "webviewFeatures" to featureMatrix(),
            "display" to displayInfo(activity),
            "inputDevices" to inputDevices(activity),
            "session" to jsonObject(
                "session" to SessionLog.sessionId(),
                "activityVariant" to (if (activity is dev.tvbrowse.w0harness.BrowserActivityUnlocked) "unlocked" else "locked"),
                "flavor" to BuildConfig.FLAVOR,
                "gitSha" to BuildConfig.GIT_SHA,
                "buildTime" to BuildConfig.BUILD_TIME,
                "backKeyEventsSeenInDispatch" to AppState.backKeyEventsSeen,
                "onBackPressedCallbackInvocations" to AppState.backCallbackInvocations,
                "hudToggles" to AppState.hudToggles,
                "coldStartToFirstFrameMs" to AppState.coldStartFirstFrameMs,
                "firstWebViewConstructorMs" to AppState.firstWebViewConstructorMs,
            ),
        )
    }

    private fun featureMatrix(): kotlinx.serialization.json.JsonObject {
        val matrix = jsonObject(
            FEATURE_MATRIX.map { name -> name to isFeatureSupported(name) },
        )
        val multiProcessSupported = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)
        val multiProcessEnabled = if (multiProcessSupported) {
            runCatching { WebViewCompat.isMultiProcessEnabled() }.getOrNull()
        } else {
            null
        }
        return jsonObject(
            "matrix" to matrix,
            "multiProcessSupported" to multiProcessSupported,
            "multiProcessEnabled" to multiProcessEnabled,
        )
    }

    /** In-app MULTI_PROCESS state (H-7): shown on Device Info and in the export. */
    fun multiProcessState(hasWebView: Boolean): Boolean? {
        if (!hasWebView) return null
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)) return null
        return runCatching { WebViewCompat.isMultiProcessEnabled() }.getOrNull()
    }

    /**
     * Best-effort WebView renderer PID list (H-7): the renderer shows up as a
     * `<webview package>:sandboxed_process*` process of this app. Empty when the
     * platform does not expose it or when no page has rendered yet.
     */
    fun rendererProcesses(activity: Activity): List<kotlinx.serialization.json.JsonObject> {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val procs = runCatching { am.runningAppProcesses ?: emptyList() }.getOrDefault(emptyList())
        return procs
            .filter { it.processName.contains("sandboxed_process") }
            .map { jsonObject("pid" to it.pid, "name" to it.processName) }
    }

    /** Reflects WebViewFeature by constant name so the matrix cannot drift from FEATURE_MATRIX. */
    private fun isFeatureSupported(name: String): Boolean? = runCatching {
        val field = WebViewFeature::class.java.getField(name)
        WebViewFeature.isFeatureSupported(field.get(null) as String)
    }.getOrNull()

    private fun displayInfo(activity: Activity): kotlinx.serialization.json.JsonObject {
        @Suppress("DEPRECATION")
        val display: Display? = activity.windowManager.defaultDisplay
        val metrics = activity.resources.displayMetrics
        return jsonObject(
            "resolutionPixels" to (display?.mode?.let { "${it.physicalWidth}x${it.physicalHeight}" }),
            "currentMode" to display?.mode?.toString(),
            "refreshRateHz" to display?.refreshRate,
            "supportedModes" to display?.supportedModes?.map { "${it.physicalWidth}x${it.physicalHeight}@${it.refreshRate}" },
            "densityDpi" to metrics.densityDpi,
            "density" to metrics.density,
            "hdr" to runCatching {
                val hdr = display?.hdrCapabilities
                jsonObject(
                    "supported" to (hdr != null),
                    "supportedTypes" to hdr?.supportedHdrTypes?.toList(),
                )
            }.getOrDefault(jsonObject("error" to "unavailable")),
        )
    }

    private fun inputDevices(activity: Activity): kotlinx.serialization.json.JsonObject {
        val im = activity.getSystemService(Context.INPUT_SERVICE) as android.hardware.input.InputManager
        val ids: IntArray = im.getInputDeviceIds()
        val deviceJsons = mutableListOf<kotlinx.serialization.json.JsonObject>()
        for (id in ids) {
            val d: InputDevice? = InputDevice.getDevice(id)
            if (d == null) continue
            val isExternal: Boolean? = if (Build.VERSION.SDK_INT >= 29) d.isExternal else null
            deviceJsons.add(
                jsonObject(
                    "id" to d.id,
                    "name" to d.name,
                    "sources" to sourcesOf(d.sources),
                    "vendorId" to d.vendorId,
                    "productId" to d.productId,
                    "isExternal" to isExternal,
                ),
            )
        }
        return jsonObject(
            "count" to deviceJsons.size,
            "devices" to deviceJsons,
        )
    }

    private fun sourcesOf(sources: Int): List<String> {
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

    private fun versionCodeOf(info: PackageInfo?): Long? = when {
        info == null -> null
        Build.VERSION.SDK_INT >= 28 -> info.longVersionCode
        else -> @Suppress("DEPRECATION") info.versionCode.toLong()
    }
}
