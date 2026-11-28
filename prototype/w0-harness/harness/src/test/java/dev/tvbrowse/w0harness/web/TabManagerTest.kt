package dev.tvbrowse.w0harness.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit (JVM): live-instance accounting, tab cap, switch + JSON round-trip (brief §8). */
class TabManagerTest {

    @Test
    fun `open makes the new tab active and records its url`() {
        val manager = TabManager()
        val tab = manager.openTab("https://example.com/") ?: throw AssertionError("open must succeed")
        assertEquals(tab.id, manager.activeTabId)
        assertEquals("https://example.com/", manager.activeTab()?.currentUrl)
        assertEquals(1, manager.tabCount)
    }

    @Test
    fun `live accounting - MRU keeps at most two tabs live`() {
        val manager = TabManager().apply { standbyMode = StandbyMode.MRU }
        val t1 = manager.openTab("https://one/")!!
        val t2 = manager.openTab("https://two/")!!
        val t3 = manager.openTab("https://three/")!!
        assertEquals(setOf(t2.id, t3.id), manager.liveTabIds())
        assertFalse(manager.liveTabIds().contains(t1.id))
        // If the pool still held all three, t1 is the eviction candidate.
        assertEquals(listOf(t1.id), manager.evictionCandidates(setOf(t1.id, t2.id, t3.id)))
    }

    @Test
    fun `live accounting - NONE keeps only the active tab live`() {
        val manager = TabManager().apply { standbyMode = StandbyMode.NONE }
        val t1 = manager.openTab("https://one/")!!
        val t2 = manager.openTab("https://two/")!!
        assertEquals(setOf(t2.id), manager.liveTabIds())
        assertEquals(listOf(t1.id), manager.evictionCandidates(setOf(t1.id, t2.id)))
    }

    @Test
    fun `eviction candidate is the oldest live non-active tab (MRU)`() {
        val manager = TabManager().apply { standbyMode = StandbyMode.MRU }
        val t1 = manager.openTab("https://one/")!!
        val t2 = manager.openTab("https://two/")!!
        val t3 = manager.openTab("https://three/")!!
        // Live right now: t2 (MRU) + t3 (active). If the pool held all three, t1 is oldest.
        manager.switchTo(t1.id) // now live = t3 (MRU) + t1 (active); t2 must be the eviction candidate
        assertEquals(setOf(t1.id, t3.id), manager.liveTabIds())
        assertEquals(listOf(t2.id), manager.evictionCandidates(setOf(t1.id, t2.id, t3.id)))
        assertFalse(manager.liveTabIds().contains(t2.id))
        assertTrue(manager.evictionCandidates(setOf(t1.id, t3.id)).isEmpty())
    }

    @Test
    fun `tab cap at 10 denies the 11th`() {
        val manager = TabManager()
        for (i in 1..TabManager.MAX_TABS) manager.openTab("https://t$i/")
        assertNull(manager.openTab("https://overflow/"))
        assertEquals(TabManager.MAX_TABS, manager.tabCount)
    }

    @Test
    fun `closeTab activates the newest remaining tab`() {
        val manager = TabManager()
        val t1 = manager.openTab("https://one/")!!
        val t2 = manager.openTab("https://two/")!!
        manager.closeTab(t2.id)
        assertEquals(t1.id, manager.activeTabId)
        assertNull(manager.closeTab(999))
        manager.closeTab(t1.id)
        assertNull(manager.activeTabId)
        assertEquals(0, manager.tabCount)
    }

    @Test
    fun `app-owned back and forward update the stored tab`() {
        val manager = TabManager()
        val t1 = manager.openTab("https://a/")!!
        manager.updateTab(t1.id) { it.pushUrl("https://b/") }
        assertTrue(manager.canBack(t1.id))
        manager.back(t1.id)
        assertEquals("https://a/", manager.tab(t1.id)?.currentUrl)
        manager.forward(t1.id)
        assertEquals("https://b/", manager.tab(t1.id)?.currentUrl)
    }

    @Test
    fun `json round-trip restores tabs, active id and standby mode`() {
        val manager = TabManager().apply { standbyMode = StandbyMode.MRU }
        val t1 = manager.openTab("https://one/")!!
        val t2 = manager.openTab("https://two/")!!
        manager.updateTab(t1.id) { it.copy(scrollY = 300) }
        val payload = manager.toJson()

        val restored = TabManager.fromJson(payload) ?: throw AssertionError("round-trip must succeed")
        assertEquals(2, restored.tabCount)
        assertEquals(t2.id, restored.activeTabId)
        assertEquals(StandbyMode.MRU, restored.standbyMode)
        assertEquals("https://one/", restored.tab(t1.id)?.currentUrl)
        assertEquals(300, restored.tab(t1.id)?.scrollY)
        assertEquals(restored.liveTabIds(), manager.liveTabIds())
    }

    @Test
    fun `fromJson returns null for garbage or empty payloads`() {
        assertNull(TabManager.fromJson("not json"))
        assertNull(TabManager.fromJson("{\"tabs\":[],\"activeId\":1}"))
    }
}
