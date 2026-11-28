package dev.tvbrowse.w0harness.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The exit detector replays the last 50 lines of the dead session (brief §5.4). */
class LogTailTest {

    private fun lines(n: Int): String = (1..n).joinToString("\n") { "line$it" }

    @Test
    fun `returns at most 50 lines`() {
        val tail = LogTail.lastLines(lines(60), 50)
        assertEquals(50, tail.size)
        assertEquals("line11", tail.first())
        assertEquals("line60", tail.last())
    }

    @Test
    fun `fewer lines than the limit returns all`() {
        val tail = LogTail.lastLines(lines(3), 50)
        assertEquals(3, tail.size)
    }

    @Test
    fun `empty input returns empty`() {
        assertTrue(LogTail.lastLines("", 50).isEmpty())
        assertTrue(LogTail.lastLines("\n\n\n", 50).isEmpty())
    }

    @Test
    fun `non-positive limit returns empty`() {
        assertTrue(LogTail.lastLines(lines(5), 0).isEmpty())
        assertTrue(LogTail.lastLines(lines(5), -1).isEmpty())
    }

    @Test
    fun `windows line endings are trimmed`() {
        val tail = LogTail.lastLines("a\r\nb\r\n", 10)
        assertEquals(listOf("a", "b"), tail)
    }
}
