package dev.tvbrowse.w0harness.web

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** T-05 standby mode setting (brief §5.3). */
enum class StandbyMode(val id: String) {
    /** A blank prewarm WebView stays warm next to the active tab. */
    PREWARM("prewarm"),

    /** The most-recently-used other tab keeps its live WebView. */
    MRU("mru"),

    /** Only the active tab is live. */
    NONE("none");

    companion object {
        fun fromId(raw: String?): StandbyMode? = entries.firstOrNull { it.id == raw?.trim()?.lowercase() }
    }
}

/**
 * Pure tab manager (brief §5.3): up to [maxTabs] tabs, at most [maxLive] live
 * WebViews (active + standby). No Android imports, so the live-instance
 * accounting and the JSON round-trip are unit-testable (brief §8).
 *
 * "Live" here means tabs that keep a WebView instance: always the active tab,
 * plus — in MRU mode — the second-most-recently-activated tab. The blank
 * prewarm WebView of PREWARM mode is pool-level and not a tab.
 */
class TabManager(
    private val maxTabs: Int = MAX_TABS,
    private val maxLive: Int = MAX_LIVE,
) {
    private val tabs = LinkedHashMap<Int, TabState>()
    private val lastActivation = HashMap<Int, Long>()
    private var activationSeq = 0L
    private var activeId: Int? = null

    var standbyMode: StandbyMode = StandbyMode.PREWARM

    val activeTabId: Int?
        get() = activeId

    val tabCount: Int
        get() = tabs.size

    val allTabs: List<TabState>
        get() = tabs.values.toList()

    fun tab(id: Int): TabState? = tabs[id]

    fun activeTab(): TabState? = activeId?.let { tabs[it] }

    /** Opens a new tab (optionally with a first URL) and makes it active. Null at the cap. */
    fun openTab(url: String?): TabState? {
        if (tabs.size >= maxTabs) return null
        val tab = TabState(id = TabState.newId()).let { if (url != null) it.pushUrl(url) else it }
        tabs[tab.id] = tab
        activate(tab.id)
        return tab
    }

    /** Closes a tab; when it was active, the newest remaining tab becomes active. */
    fun closeTab(id: Int): TabState? {
        val removed = tabs.remove(id) ?: return null
        lastActivation.remove(id)
        if (activeId == id) {
            activeId = tabs.keys.maxOrNull()
            activeId?.let { activate(it) }
        }
        return removed
    }

    /** Switches the active tab. Returns false for an unknown id. */
    fun switchTo(id: Int): Boolean {
        if (!tabs.containsKey(id)) return false
        activate(id)
        return true
    }

    /** Mutates one tab through [transform]. Defaults to the active tab. */
    fun updateTab(id: Int? = activeId, transform: (TabState) -> TabState) {
        val target = id ?: return
        tabs[target]?.let { tabs[target] = transform(it) }
    }

    /** App-owned back on a tab; returns the state BEFORE the step (new state via [tab]). */
    fun back(id: Int? = activeId): TabState? = tabs[id]?.back()?.also { tabs[id!!] = it }

    /** App-owned forward on a tab. */
    fun forward(id: Int? = activeId): TabState? = tabs[id]?.forward()?.also { tabs[id!!] = it }

    fun canBack(id: Int? = activeId): Boolean = (tabs[id]?.index ?: -1) > 0

    fun canForward(id: Int? = activeId): Boolean {
        val t = tabs[id] ?: return false
        return t.index < t.urlStack.lastIndex
    }

    /**
     * Tab ids that may keep a live WebView right now: the active tab plus, in
     * MRU mode, the most recently activated other tab. Never more than
     * [maxLive] (brief §8 accounting).
     */
    fun liveTabIds(): Set<Int> {
        val active = activeId ?: return emptySet()
        return when (standbyMode) {
            StandbyMode.NONE -> setOf(active)
            StandbyMode.PREWARM -> setOf(active)
            StandbyMode.MRU ->
                lastActivation.entries
                    .filter { it.key != active }
                    .sortedByDescending { it.value }
                    .take(maxLive - 1)
                    .map { it.key }
                    .toSet() + active
        }
    }

    /**
     * Live-but-not-allowed tab ids, oldest activation first — the candidates
     * the pool destroys (AA §6.3) when the live set must shrink.
     */
    fun evictionCandidates(liveIds: Set<Int>): List<Int> {
        val allowed = liveTabIds()
        return liveIds.filter { it !in allowed }.sortedBy { lastActivation[it] ?: 0L }
    }

    private fun activate(id: Int) {
        activeId = id
        lastActivation[id] = ++activationSeq
    }

    // ------------------------------------------------------------ persistence

    /** Serialized tab state (brief §5.5): tabs, active id, standby mode. */
    fun toJson(): String = json.encodeToString(
        Snapshot(
            tabs = tabs.values.toList(),
            activeId = activeId,
            standbyMode = standbyMode.id,
        ),
    )

    /** Restores a manager from JSON; null when the payload is unusable. */
    fun restoreFromJson(payload: String): Boolean {
        val snap = runCatching { json.decodeFromString<Snapshot>(payload) }.getOrNull() ?: return false
        if (snap.tabs.isEmpty()) return false
        tabs.clear()
        lastActivation.clear()
        snap.tabs.forEach { tabs[it.id] = it }
        snap.tabs.forEachIndexed { i, t -> lastActivation[t.id] = (snap.tabs.size - i).toLong() }
        activeId = snap.activeId?.takeIf { tabs.containsKey(it) } ?: tabs.keys.maxOrNull()
        activeId?.let { activate(it) }
        standbyMode = StandbyMode.fromId(snap.standbyMode) ?: StandbyMode.PREWARM
        // Future openTab calls must not collide with restored ids.
        TabState.ensureCounterAbove(tabs.keys.maxOrNull() ?: 0)
        return true
    }

    @Serializable
    data class Snapshot(
        val tabs: List<TabState>,
        val activeId: Int? = null,
        val standbyMode: String = "prewarm",
        val savedAtMs: Long = 0L,
    )

    companion object {
        const val MAX_TABS = 10
        const val MAX_LIVE = 2

        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(payload: String): TabManager? {
            val manager = TabManager()
            return if (manager.restoreFromJson(payload)) manager else null
        }
    }
}
