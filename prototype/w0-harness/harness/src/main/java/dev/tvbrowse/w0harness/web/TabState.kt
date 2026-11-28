package dev.tvbrowse.w0harness.web

import kotlinx.serialization.Serializable

/**
 * App-owned per-tab state (brief §5.3/§5.5). Pure data: the URL stack + index +
 * scrollY is the source of truth for stored tabs and for session restore after
 * process death (T-12). Serialization is JSON (kotlinx-serialization).
 */
@Serializable
data class TabState(
    val id: Int,
    val urlStack: List<String> = emptyList(),
    val index: Int = -1,
    val scrollY: Int = 0,
    val title: String? = null,
) {
    /** The URL the tab is currently on (null for a fresh, never-navigated tab). */
    val currentUrl: String?
        get() = urlStack.getOrNull(index)

    /**
     * Top-level navigation: truncates the forward stack and appends. Navigating
     * to the URL already on top (redirects, fragment scrolls, reloads) is a
     * no-op so the stack only records distinct top-level entries.
     */
    fun pushUrl(url: String): TabState =
        if (urlStack.getOrNull(index) == url) {
            this
        } else {
            copy(urlStack = urlStack.take(index + 1) + url, index = index + 1)
        }

    /** App-owned back; null when there is nothing behind the current entry. */
    fun back(): TabState? = if (index > 0) copy(index = index - 1) else null

    /** App-owned forward; null when the current entry is the newest. */
    fun forward(): TabState? = if (index < urlStack.lastIndex) copy(index = index + 1) else null

    companion object {
        private val counter = java.util.concurrent.atomic.AtomicInteger(1)

        /** Process-unique tab id (1-based, stable within one app process). */
        fun newId(): Int = counter.getAndIncrement()

        /** Keeps ids unique after a restore pulled stored ids into this process. */
        fun ensureCounterAbove(id: Int) {
            counter.updateAndGet { current -> if (current > id) current else id + 1 }
        }
    }
}
