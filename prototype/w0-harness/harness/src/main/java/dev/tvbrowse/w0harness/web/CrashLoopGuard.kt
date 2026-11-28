package dev.tvbrowse.w0harness.web

/**
 * Crash-loop guard (brief §5.4): at most [maxRecoveries] renderer-crash
 * recoveries inside [windowMs]. The next crash inside the window trips the
 * guard and shows a D-pad-dismissible error page instead of recreating again.
 * Pure logic, unit-tested (brief §8).
 */
class CrashLoopGuard(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val maxRecoveries: Int = 2,
    private val now: () -> Long = System::currentTimeMillis,
) {
    enum class Decision { RECOVER, TRIP }

    private val recoveries = ArrayDeque<Long>()

    val windowSeconds: Int
        get() = (windowMs / 1000L).toInt()

    /**
     * Records one renderer exit at time [atMs] and decides: RECOVER while the
     * number of crashes inside the window is <= [maxRecoveries], TRIP once it
     * exceeds it (the 3rd crash inside 60 s by default).
     */
    fun recordAndDecide(atMs: Long = now()): Decision {
        recoveries.addLast(atMs)
        while (recoveries.isNotEmpty() && atMs - recoveries.first() > windowMs) {
            recoveries.removeFirst()
        }
        return if (recoveries.size > maxRecoveries) Decision.TRIP else Decision.RECOVER
    }

    /** Clears the window (e.g. after the tester dismisses the error page). */
    fun reset() = recoveries.clear()

    /** Number of crashes currently inside the window (logged with the decision). */
    fun crashesInWindow(): Int = recoveries.size

    companion object {
        const val DEFAULT_WINDOW_MS = 60_000L
    }
}
