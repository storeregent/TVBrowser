package dev.tvbrowse.w0harness.web

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit (JVM): tab-state JSON round-trip incl. the URL stack and index after back/forward (brief §8). */
class TabStateTest {

    @Test
    fun `pushUrl builds a stack and back-forward moves the index`() {
        var tab = TabState(id = 1)
        assertNull(tab.currentUrl)

        tab = tab.pushUrl("https://a.example/").pushUrl("https://b.example/").pushUrl("https://c.example/")
        assertEquals("https://c.example/", tab.currentUrl)
        assertEquals(2, tab.index)
        assertEquals(3, tab.urlStack.size)

        tab = tab.back() ?: throw AssertionError("back must exist")
        assertEquals("https://b.example/", tab.currentUrl)
        tab = tab.back() ?: throw AssertionError("second back must exist")
        assertEquals("https://a.example/", tab.currentUrl)
        assertNull(tab.back())

        tab = tab.forward() ?: throw AssertionError("forward must exist")
        assertEquals("https://b.example/", tab.currentUrl)
    }

    @Test
    fun `pushUrl after back truncates the forward stack`() {
        var tab = TabState(id = 1)
            .pushUrl("https://a.example/")
            .pushUrl("https://b.example/")
            .pushUrl("https://c.example/")
            .back()!!
        tab = tab.pushUrl("https://d.example/")
        assertEquals(listOf("https://a.example/", "https://b.example/", "https://d.example/"), tab.urlStack)
        assertNull(tab.forward())
    }

    @Test
    fun `navigating to the current top is a no-op (redirects, reloads)`() {
        val tab = TabState(id = 1).pushUrl("https://a.example/")
        assertEquals(tab, tab.pushUrl("https://a.example/"))
    }

    @Test
    fun `json round-trip keeps stack, index, scrollY and title`() {
        val json = Json { ignoreUnknownKeys = true }
        val tab = TabState(id = 7, urlStack = listOf("https://a/", "https://b/", "https://c/"), index = 1, scrollY = 420, title = "Mid")
        val encoded = json.encodeToString(TabState.serializer(), tab)
        val decoded = json.decodeFromString(TabState.serializer(), encoded)
        assertEquals(tab, decoded)
        assertEquals(1, decoded.index)
        assertEquals("https://b/", decoded.currentUrl)
        assertEquals(420, decoded.scrollY)

        // index after back/forward survives the round-trip
        val moved = decoded.forward()!!.back()!!
        val reEncoded = json.encodeToString(TabState.serializer(), moved)
        assertEquals(moved, json.decodeFromString(TabState.serializer(), reEncoded))
    }
}
