package dev.tvbrowse.w0harness.log

import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Session header payload (the "QA packet", brief §6 / QA §7.1).
 *
 * Pure builder: plain values in, JsonObject out. The Android side collects the
 * values; the JVM unit test asserts that every required field is present.
 */
object QaPacket {

    fun build(
        deviceModel: String,
        deviceFingerprint: String,
        osRelease: String,
        osSdkInt: Int,
        osSecurityPatch: String,
        isLowRamDevice: Boolean,
        totalMemMb: Long,
        memoryClassMb: Int,
        largeMemoryClassMb: Int,
        webViewPackageName: String?,
        webViewVersionName: String?,
        webViewVersionCode: Long?,
        harnessVersion: String,
        flavor: String,
        gitSha: String,
        buildTime: String,
        activityVariant: String,
        settings: Map<String, Any?>,
    ): kotlinx.serialization.json.JsonObject = jsonObject(
        "device" to jsonObject(
            "model" to deviceModel,
            "fingerprint" to deviceFingerprint,
        ),
        "os" to jsonObject(
            "release" to osRelease,
            "sdkInt" to osSdkInt,
            "securityPatch" to osSecurityPatch,
        ),
        "ramClass" to jsonObject(
            "isLowRamDevice" to isLowRamDevice,
            "totalMemMb" to totalMemMb,
            "memoryClassMb" to memoryClassMb,
            "largeMemoryClassMb" to largeMemoryClassMb,
        ),
        "webView" to jsonObject(
            "packageName" to webViewPackageName,
            "versionName" to webViewVersionName,
            "versionCode" to webViewVersionCode,
        ),
        "harness" to jsonObject(
            "version" to harnessVersion,
            "flavor" to flavor,
            "gitSha" to gitSha,
            "buildTime" to buildTime,
            "activityVariant" to activityVariant,
        ),
        "settings" to jsonObject(settings.map { it.key to it.value }),
    )
}
