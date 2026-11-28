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
 * JSONL session logging (brief §6):
 *  - one file per app session in getExternalFilesDir("w0logs"),
 *  - named w0h_<deviceModel>_<yyyyMMdd-HHmmss>.jsonl,
 *  - every line is a [LogLine] and is mirrored to logcat with tag `W0H`,
 *  - the first line is the QA packet (SESSION_HEADER).
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
            logsDir = (external ?: File(context.filesDir, "w0logs")).apply { mkdirs() }
            sessionFile = File(
                logsDir,
                "w0h_${sanitize(Build.MODEL)}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.jsonl",
            )

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
            writeLine(LogLine.at(System.currentTimeMillis(), sessionId, "SESSION_HEADER", header))
            if (external == null) {
                log("STORAGE_FALLBACK", jsonObject("reason" to "getExternalFilesDir returned null; logs are in internal storage"))
            }
        }
    }

    fun log(event: String, data: JsonObject = jsonObject()) {
        val line = LogLine.now(sessionId, event, data)
        synchronized(lock) {
            if (initialized) {
                runCatching { sessionFile.appendText(line.toJson() + "\n") }
                    .onFailure { Log.e(LOGCAT_TAG, "append failed: ${it.message}") }
            }
        }
        Log.i(LOGCAT_TAG, line.toJson())
    }

    fun mark(label: String) = log("MARK", jsonObject("label" to label))

    fun sessionId(): String = sessionId

    fun sessionFilePath(): String = if (initialized) sessionFile.absolutePath else ""

    fun logsDirectoryPath(): String = if (initialized) logsDir.absolutePath else ""

    fun listLogFiles(): List<File> = logsDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    /** Writes device_info_<ts>.json next to the session logs (brief §5.1). */
    fun exportDeviceInfo(info: JsonObject): File {
        val target = File(logsDir, "device_info_${fileTimestamp()}.json")
        target.writeText(info.toString())
        log("DEVICE_INFO_EXPORTED", jsonObject("path" to target.absolutePath, "bytes" to target.length()))
        return target
    }

    /** "Export all" (brief §6): writes a single zip with every log file into the same directory. */
    fun exportAll(): File {
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
        return zip
    }

    private fun writeLine(line: LogLine) {
        sessionFile.appendText(line.toJson() + "\n")
        Log.i(LOGCAT_TAG, line.toJson())
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
