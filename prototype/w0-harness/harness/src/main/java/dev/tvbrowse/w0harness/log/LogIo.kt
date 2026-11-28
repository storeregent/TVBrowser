package dev.tvbrowse.w0harness.log

import java.util.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Single-thread background executor for ALL harness file I/O (M1.1 D-4).
 *
 *  - One worker thread ⇒ FIFO execution ⇒ event order is preserved.
 *  - `flushSync` waits (bounded, default <= 500 ms) until everything queued so
 *    far has been written — used in onStop, on export and in the uncaught
 *    exception handler so a crash does not lose the last lines.
 *  - The thread is a daemon so it never blocks process exit.
 */
object LogIo {

    private val executor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        { runnable ->
            Thread(runnable, "W0H-LogIo").apply { isDaemon = true }
        },
    )

    fun execute(block: () -> Unit) {
        runCatching { executor.execute(block) }
    }

    /** Waits until all queued work is done; false on timeout or shutdown. */
    fun flushSync(timeoutMs: Long = 500): Boolean = try {
        executor.submit {}.get(timeoutMs, TimeUnit.MILLISECONDS)
        true
    } catch (_: Exception) {
        false
    }
}
