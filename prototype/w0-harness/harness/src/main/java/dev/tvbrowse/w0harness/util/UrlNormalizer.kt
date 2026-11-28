package dev.tvbrowse.w0harness.util

import java.net.URI

/**
 * Home-screen URL handling (brief §4.1): "Plain text is treated as a URL if it
 * parses; otherwise it is ignored (no search engine call)."
 *
 * Pure JVM logic (unit-testable). Returns the normalized URL, or null when the
 * input must be ignored.
 */
object UrlNormalizer {

    private val KNOWN_SCHEMES = listOf("http://", "https://", "about:", "data:", "file://")

    fun normalize(raw: String): String? {
        val input = raw.trim()
        if (input.isEmpty()) return null
        if (input.any { it.isWhitespace() }) return null // never a search query: ignore
        val lower = input.lowercase()
        if (KNOWN_SCHEMES.any { lower.startsWith(it) }) return input
        if (!input.contains('.')) return null
        val candidate = "https://$input"
        return try {
            val uri = URI(candidate)
            val host = uri.host
            if (host != null && host.contains('.')) candidate else null
        } catch (_: Exception) {
            null
        }
    }
}
