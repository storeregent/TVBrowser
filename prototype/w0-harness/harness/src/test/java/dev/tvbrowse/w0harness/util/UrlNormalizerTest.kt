package dev.tvbrowse.w0harness.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Brief §4.1: "Plain text is treated as a URL if it parses; otherwise it is
 * ignored (no search engine call, because rule 5 forbids network calls the
 * tester did not request)."
 */
class UrlNormalizerTest {

    @Test
    fun `http and https urls pass through`() {
        assertEquals("https://example.com/", UrlNormalizer.normalize("https://example.com/"))
        assertEquals("http://example.com/test", UrlNormalizer.normalize("http://example.com/test"))
    }

    @Test
    fun `about data and file schemes pass through`() {
        assertEquals("about:blank", UrlNormalizer.normalize("about:blank"))
        assertEquals("data:text/html,<h1>x</h1>", UrlNormalizer.normalize("data:text/html,<h1>x</h1>"))
        assertEquals("file:///sdcard/page.html", UrlNormalizer.normalize("file:///sdcard/page.html"))
    }

    @Test
    fun `bare domain gets https prefix`() {
        assertEquals("https://example.com", UrlNormalizer.normalize("example.com"))
        assertEquals("https://example.com/page", UrlNormalizer.normalize("example.com/page"))
        assertEquals("https://192.168.1.50:8080/", UrlNormalizer.normalize("192.168.1.50:8080/"))
    }

    @Test
    fun `plain text without dot is ignored`() {
        assertNull(UrlNormalizer.normalize("best tv browser"))
        assertNull(UrlNormalizer.normalize("hello"))
        assertNull(UrlNormalizer.normalize(""))
        assertNull(UrlNormalizer.normalize("   "))
    }

    @Test
    fun `whitespace inside the input means ignore, never a search`() {
        assertNull(UrlNormalizer.normalize("example .com"))
    }

    @Test
    fun `garbage with a dot but no host is ignored`() {
        assertNull(UrlNormalizer.normalize("..."))
    }
}
