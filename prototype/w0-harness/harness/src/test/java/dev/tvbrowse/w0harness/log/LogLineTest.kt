package dev.tvbrowse.w0harness.log

import dev.tvbrowse.w0harness.util.jsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Brief §8: "Log-line serializer: required fields present". */
class LogLineTest {

    @Test
    fun `every serialized line contains the five required fields`() {
        val line = LogLine.at(
            timestampMs = 1_700_000_000_000,
            session = "s-abcd1234",
            event = "MARK",
            data = jsonObject("label" to "P1: remote asleep"),
        )
        val parsed = Json.parseToJsonElement(line.toJson()).jsonObject
        assertEquals(
            setOf("ts", "iso", "session", "event", "data"),
            parsed.keys,
        )
        assertEquals("MARK", parsed["event"]!!.toString().trim('"'))
        assertEquals("s-abcd1234", parsed["session"]!!.toString().trim('"'))
        assertNotNull(parsed["data"]!!.jsonObject["label"])
    }

    @Test
    fun `iso timestamp matches ts`() {
        val line = LogLine.at(0, "s-1", "TEST", jsonObject())
        assertEquals("1970-01-01T00:00:00Z", line.iso)
    }

    @Test
    fun `round trip preserves data object`() {
        val line = LogLine.now("s-x", "KEY_EVENT", jsonObject("code" to "KEYCODE_BACK", "repeat" to 2, "longPress" to false))
        val restored = LogLine.JSON.decodeFromString(LogLine.serializer(), line.toJson())
        assertEquals(line, restored)
    }

    @Test
    fun `session id has the s- prefix`() {
        val id = LogLine.newSessionId()
        assertTrue(id.startsWith("s-"))
        assertEquals(10, id.length)
    }
}
