package dev.tvbrowse.w0harness.log

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.UUID

/**
 * One JSONL log line (brief §6). Every line has exactly these five fields:
 * `ts` (ms epoch), `iso` (ISO-8601 of ts), `session`, `event` and `data`.
 *
 * Pure JVM logic so it can be unit-tested without a device (brief §8).
 */
@Serializable
data class LogLine(
    val ts: Long,
    val iso: String,
    val session: String,
    val event: String,
    val data: JsonObject,
) {
    fun toJson(): String = JSON.encodeToString(serializer(), this)

    companion object {
        val JSON: Json = Json { encodeDefaults = true }

        const val REQUIRED_FIELDS: String = "ts,iso,session,event,data"

        /** Builds a line stamped at [timestampMs]. */
        fun at(timestampMs: Long, session: String, event: String, data: JsonObject): LogLine = LogLine(
            ts = timestampMs,
            iso = IsoTime.format(timestampMs),
            session = session,
            event = event,
            data = data,
        )

        /** Builds a line stamped at the current wall clock. */
        fun now(session: String, event: String, data: JsonObject, clock: () -> Long = System::currentTimeMillis): LogLine =
            at(clock(), session, event, data)

        /** Short session id used in every line of one app session. */
        fun newSessionId(): String = "s-" + UUID.randomUUID().toString().replace("-", "").take(8)
    }
}

object IsoTime {
    fun format(epochMs: Long): String = Instant.ofEpochMilli(epochMs).toString()
}
