package dev.tvbrowse.w0harness

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.ExitMarker
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Application: session bootstrap (brief §6).
 *
 * Order matters for the unexpected-exit detector (§5.4):
 *  1. capture cold-start t0 (T-18),
 *  2. read the still-open `session_open` marker of the previous process (if any),
 *  3. init the session log (writes SESSION_HEADER),
 *  4. log PREVIOUS_SESSION_DIED + the last 50 lines of the dead session,
 *  5. write a fresh marker; cleared when the last activity finishes cleanly.
 */
class HarnessApplication : Application() {

    private val lifecycleWatcher = object : ActivityLifecycleCallbacks {
        private var startedCount = 0

        override fun onActivityStarted(activity: Activity) {
            startedCount++
        }

        override fun onActivityStopped(activity: Activity) {
            startedCount--
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity.isFinishing && startedCount <= 0) {
                SessionLog.log("SESSION_END", jsonObject("reason" to "last activity finished cleanly"))
                ExitMarker.clear(this@HarnessApplication)
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    override fun onCreate() {
        AppState.appStartElapsedMs = SystemClock.elapsedRealtime()
        super.onCreate()

        val settings = SettingsStore(this)
        val previous = ExitMarker.readPrevious(this)

        SessionLog.init(this, settings.activityVariant(), settings.snapshot())

        previous?.let { dead ->
            SessionLog.log(
                "PREVIOUS_SESSION_DIED",
                jsonObject(
                    "previousSession" to dead.session,
                    "markerTsMs" to dead.markerTsMs,
                    "previousLogFile" to dead.logFilePath,
                    "lastLines" to dead.lastLines,
                    "hint" to "session_open marker was not cleared: the previous process exited unexpectedly (C-06 detector)",
                ),
            )
        }

        ExitMarker.write(
            this,
            session = SessionLog.sessionId(),
            tsMs = System.currentTimeMillis(),
            logFilePath = SessionLog.sessionFilePath(),
        )

        registerActivityLifecycleCallbacks(lifecycleWatcher)
        SessionLog.log(
            "APP_START",
            jsonObject(
                "versionName" to BuildConfig.VERSION_NAME,
                "flavor" to BuildConfig.FLAVOR,
                "gitSha" to BuildConfig.GIT_SHA,
                "buildTime" to BuildConfig.BUILD_TIME,
            ),
        )
    }

    override fun onTrimMemory(level: Int) {
        SessionLog.log("TRIM_MEMORY", jsonObject("level" to trimLevelName(level)))
        super.onTrimMemory(level)
    }

    private fun trimLevelName(level: Int): String = when (level) {
        TRIM_MEMORY_COMPLETE -> "TRIM_MEMORY_COMPLETE"
        TRIM_MEMORY_MODERATE -> "TRIM_MEMORY_MODERATE"
        TRIM_MEMORY_BACKGROUND -> "TRIM_MEMORY_BACKGROUND"
        TRIM_MEMORY_UI_HIDDEN -> "TRIM_MEMORY_UI_HIDDEN"
        TRIM_MEMORY_RUNNING_CRITICAL -> "TRIM_MEMORY_RUNNING_CRITICAL"
        TRIM_MEMORY_RUNNING_LOW -> "TRIM_MEMORY_RUNNING_LOW"
        TRIM_MEMORY_RUNNING_MODERATE -> "TRIM_MEMORY_RUNNING_MODERATE"
        else -> "UNKNOWN($level)"
    }
}
