package dev.tvbrowse.w0harness.web

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit (JVM): crash-loop guard window, count, 3rd-crash behaviour (brief §8). */
class CrashLoopGuardTest {

    @Test
    fun `two recoveries inside the window are allowed`() {
        val guard = CrashLoopGuard(windowMs = 60_000, maxRecoveries = 2, now = { 0 })
        assertEquals(CrashLoopGuard.Decision.RECOVER, guard.recordAndDecide(1_000))
        assertEquals(CrashLoopGuard.Decision.RECOVER, guard.recordAndDecide(2_000))
        assertEquals(2, guard.crashesInWindow())
    }

    @Test
    fun `third crash inside the window trips the guard`() {
        val guard = CrashLoopGuard(windowMs = 60_000, maxRecoveries = 2, now = { 0 })
        guard.recordAndDecide(1_000)
        guard.recordAndDecide(2_000)
        assertEquals(CrashLoopGuard.Decision.TRIP, guard.recordAndDecide(3_000))
        assertEquals(3, guard.crashesInWindow())
    }

    @Test
    fun `crashes outside the window do not count`() {
        val guard = CrashLoopGuard(windowMs = 60_000, maxRecoveries = 2, now = { 0 })
        guard.recordAndDecide(0)
        guard.recordAndDecide(1_000)
        // 90 s later the two earlier crashes left the 60 s window: recover again.
        assertEquals(CrashLoopGuard.Decision.RECOVER, guard.recordAndDecide(91_000))
        assertEquals(1, guard.crashesInWindow())
    }

    @Test
    fun `boundary - crash exactly at the window edge is outside`() {
        val guard = CrashLoopGuard(windowMs = 60_000, maxRecoveries = 2, now = { 0 })
        guard.recordAndDecide(0)
        guard.recordAndDecide(60_000) // first is now older than the window (> windowMs)
        guard.recordAndDecide(61_000)
        assertEquals(CrashLoopGuard.Decision.RECOVER, guard.recordAndDecide(62_000))
    }

    @Test
    fun `reset clears the window`() {
        val guard = CrashLoopGuard(windowMs = 60_000, maxRecoveries = 2, now = { 0 })
        guard.recordAndDecide(1_000)
        guard.recordAndDecide(2_000)
        guard.reset()
        assertEquals(0, guard.crashesInWindow())
        assertEquals(CrashLoopGuard.Decision.RECOVER, guard.recordAndDecide(3_000))
    }
}
