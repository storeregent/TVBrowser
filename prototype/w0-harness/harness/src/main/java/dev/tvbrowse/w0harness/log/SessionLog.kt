package dev.tvbrowse.w0harness.log

import android.content.Context
import android.os.Build
import android.util.Log
import dev.tvbrowse.w0harness.BuildConfig
import dev.tvbrowse.w0harness.util.jsonObject
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * JSONL session logging (brief §6, M1.1 D-4):
 *  - one file per app session in getExternalFilesDir("w0logs"),
 *  - named w0h_<deviceModel>_<yyyyMMdd-HHmmss>.jsonl,
 *  - every line is a [LogLine] and is mirrored to logcat with tag `W0H`,
 *  - the first line is the QA packet (SESSION_HEADER),
 *  - file I/O happens on the single [LogIo] thread, never on the calling
 *    (main) thread. Timestamps are taken at call time and the single worker
 *    preserves event order.
 */
object SessionLog {

    private const val LOGCAT_TAG = "W0H"

    private val lock = Any()
    private var initialized = false
    private var sessionId: String = ""
    private lateinit var logsDir: File
    private lateinit var sessionFile: File

    fun init(context: Context, activityVariant: String, settings: Map<String, Any?>) {
        synchronized(lock) {
            if (initialized) return
            initialized = true
            sessionId = LogLine.newSessionId()
            val external = context.getExternalFilesDir("w0logs")
            logsDir = (external ?: File(context.filesDir, "w0logs"))
            sessionFile = File(
                logsDir,
                "w0h_${sanitize(Build.MODEL)}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.jsonl",
            )

            // Header data is collected on the calling thread (cheap, in-memory);
            // the file write goes through LogIo (off main thread, first in queue).
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val wvPackage = runCatching {
                androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)
            }.getOrNull()

            val header = QaPacket.build(
                deviceModel = Build.MODEL,
                deviceFingerprint = Build.FINGERPRINT,
                osRelease = Build.VERSION.RELEASE,
                osSdkInt = Build.VERSION.SDK_INT,
                osSecurityPatch = Build.VERSION.SECURITY_PATCH,
                isLowRamDevice = am.isLowRamDevice(),
                totalMemMb = memInfo.totalMem / (1024L * 1024L),
                memoryClassMb = am.memoryClass,
                largeMemoryClassMb = am.largeMemoryClass,
                webViewPackageName = wvPackage?.packageName,
                webViewVersionName = wvPackage?.versionName,
                webViewVersionCode = versionCodeOf(wvPackage),
                harnessVersion = BuildConfig.VERSION_NAME,
                flavor = BuildConfig.FLAVOR,
                gitSha = BuildConfig.GIT_SHA,
                buildTime = BuildConfig.BUILD_TIME,
                activityVariant = activityVariant,
                settings = settings,
            )
            val line = LogLine.at(System.currentTimeMillis(), sessionId, "SESSION_HEADER", header)
            LogIo.execute {
                runCatching {
                    logsDir.mkdirs()
                    sessionFile.appendText(line.toJson() + "\n")
                    if (external == null) {
                        appendDirect(
                            LogLine.now(sessionId, "STORAGE_FALLBACK", jsonObject("reason" to "getExternalFilesDir returned null; logs are in internal storage")),
                        )
                    }
                }.onFailure { Log.e(LOGCAT_TAG, "header write failed: ${it.message}") }
            }
            Log.i(LOGCAT_TAG, line.toJson())
        }
    }

    fun log(event: String, data: JsonObject = jsonObject()) {
        // Timestamp + serialization at CALL time (D-4); the write is queued.
        val line = LogLine.now(sessionId, event, data)
        Log.i(LOGCAT_TAG, line.toJson()) // synchronous logcat mirror (allowed)
        if (!initialized) return
        LogIo.execute {
            runCatching { appendDirect(line) }
                .onFailure { Log.e(LOGCAT_TAG, "append failed: ${it.message}") }
        }
    }

    fun mark(label: String) = log("MARK", jsonObject("label" to label))

    fun sessionId(): String = sessionId

    fun sessionFilePath(): String = if (initialized) sessionFile.absolutePath else ""

    fun logsDirectoryPath(): String = if (initialized) logsDir.absolutePath else ""

    fun listLogFiles(): List<File> = logsDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    /**
     * Writes device_info_<ts>.json next to the session logs (brief §5.1).
     * Callers must invoke this off the main thread (the UI uses Dispatchers.IO);
     * the session file is flushed first so the export contains every line.
     */
    fun exportDeviceInfo(info: JsonObject): File {
        LogIo.flushSync()
        val target = File(logsDir, "device_info_${fileTimestamp()}.json")
        target.writeText(info.toString())
        log("DEVICE_INFO_EXPORTED", jsonObject("path" to target.absolutePath, "bytes" to target.length()))
        LogIo.flushSync()
        return target
    }

    /** "Export all" (brief §6): writes a single zip with every log file into the same directory. */
    fun exportAll(): File {
        LogIo.flushSync()
        val zip = File(logsDir, "w0h_export_${fileTimestamp()}.zip")
        val files = listLogFiles().filter { it != zip }
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            for (f in files) {
                zos.putNextEntry(ZipEntry(f.name))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        log("EXPORT_ALL", jsonObject("zip" to zip.absolutePath, "entries" to files.size))
        LogIo.flushSync()
        return zip
    }

    /** Must be called on the LogIo thread (queue order already guarantees exclusivity). */
    private fun appendDirect(line: LogLine) {
        sessionFile.appendText(line.toJson() + "\n")
    }

    private fun versionCodeOf(info: android.content.pm.PackageInfo?): Long? = when {
        info == null -> null
        Build.VERSION.SDK_INT >= 28 -> info.longVersionCode
        else -> @Suppress("DEPRECATION") info.versionCode.toLong()
    }

    private fun sanitize(model: String): String = model.replace(Regex("[^A-Za-z0-9._-]"), "-")

    private fun fileTimestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
}
