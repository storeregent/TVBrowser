package dev.tvbrowse.w0harness.log

import android.content.Context
import dev.tvbrowse.w0harness.util.jsonObject
import java.io.File

/**
 * Unexpected-exit detector (brief §5.4, C-06):
 *  - a `session_open` marker is written when a session starts,
 *  - it is cleared when the last activity finishes cleanly,
 *  - on the next launch, if the marker is still there, the app logs
 *    PREVIOUS_SESSION_DIED together with the last 50 log lines of the dead
 *    session. This catches the single-process case where a renderer crash
 *    kills the whole app.
 *
 * Marker writes/deletes run on the [LogIo] thread (M1.1 D-4); the startup read
 * is a single small-file read at process start.
 */
object ExitMarker {

    private const val MARKER_NAME = "session_open.json"

    data class PreviousSession(
        val session: String?,
        val markerTsMs: Long?,
        val logFilePath: String?,
        val lastLines: List<String>,
    )

    private fun markerFile(context: Context): File = File(context.filesDir, MARKER_NAME)

    /** Returns the still-open previous session (if any). Does not delete the marker. */
    fun readPrevious(context: Context): PreviousSession? {
        val file = markerFile(context)
        if (!file.exists()) return null
        return runCatching {
            val json = kotlinx.serialization.json.Json.parseToJsonElement(file.readText()).let {
                it as kotlinx.serialization.json.JsonObject
            }
            fun str(key: String): String? = (json[key] as? kotlinx.serialization.json.JsonPrimitive)?.content
            val session = str("session")
            val tsMs = str("ts")?.toLongOrNull()
            val logPath = str("logFile")
            val tail = logPath?.let { path ->
                val f = File(path)
                if (f.exists()) LogTail.lastLines(f.readText(), 50) else emptyList()
            } ?: emptyList()
            PreviousSession(session, tsMs, logPath, tail)
        }.getOrNull() ?: PreviousSession(null, null, null, emptyList())
    }

    fun write(context: Context, session: String, tsMs: Long, logFilePath: String) {
        val payload = jsonObject("session" to session, "ts" to tsMs, "logFile" to logFilePath).toString()
        LogIo.execute {
            runCatching { markerFile(context).writeText(payload) }
        }
    }

    fun clear(context: Context) {
        LogIo.execute {
            runCatching { markerFile(context).delete() }
        }
    }
}
