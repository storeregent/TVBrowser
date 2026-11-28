package dev.tvbrowse.w0harness

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.StrictMode
import android.os.SystemClock
import dev.tvbrowse.w0harness.app.AppState
import dev.tvbrowse.w0harness.app.SettingsStore
import dev.tvbrowse.w0harness.log.ExitMarker
import dev.tvbrowse.w0harness.log.LogIo
import dev.tvbrowse.w0harness.log.SessionLog
import dev.tvbrowse.w0harness.util.jsonObject

/**
 * Application: session bootstrap (brief §6).
 *
 * Order matters for the unexpected-exit detector (§5.4):
 *  1. capture cold-start t0 (T-18),
 *  2. read the still-open `session_open` marker of the previous process (if any),
 *  3. init the session log (writes SESSION_HEADER via the LogIo thread),
 *  4. log PREVIOUS_SESSION_DIED + the last 50 lines of the dead session,
 *  5. write a fresh marker; cleared when the last activity finishes cleanly.
 *
 * M1.1 additions:
 *  - StrictMode `detectDiskWrites` in debug (A6): the harness must show no
 *    SessionLog disk-write violations on the main thread (D-4).
 *  - A default uncaught-exception handler that logs the crash and flushes the
 *    log writer synchronously (bounded wait) before delegating (D-4).
 */
class HarnessApplication : Application() {

    private val lifecycleWatcher = object : ActivityLifecycleCallbacks {
        private var startedCount = 0

        override fun onActivityStarted(activity: Activity) {
            startedCount++
        }

        override fun onActivityStopped(activity: Activity) {
            startedCount--
            if (startedCount <= 0) {
                SessionLog.log("APP_STOP", jsonObject("reason" to "no started activities; flushing log writer"))
                LogIo.flushSync()
            }
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

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .penaltyLog()
                    .build(),
            )
        }

        installCrashFlushHandler()

        val settings = SettingsStore(this)
        val previous = ExitMarker.readPrevious(this)
        AppState.previousSessionDied = previous != null

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

    /** D-4: log the crash and flush the writer (bounded <= 500 ms) before dying. */
    private fun installCrashFlushHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                SessionLog.log(
                    "UNCAUGHT_EXCEPTION",
                    jsonObject(
                        "thread" to thread.name,
                        "exceptionClass" to throwable.javaClass.name,
                        "message" to (throwable.message ?: ""),
                    ),
                )
                LogIo.flushSync(500)
            }
            previous?.uncaughtException(thread, throwable)
        }
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
