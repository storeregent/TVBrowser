package dev.tvbrowse.w0harness.log

/**
 * Returns the last [maxLines] non-blank lines of [text]. Used by the
 * unexpected-exit detector (C-06) to replay the tail of the previous session's
 * log into the new session file. Pure JVM logic (brief §8).
 */
object LogTail {
    fun lastLines(text: String, maxLines: Int): List<String> {
        if (maxLines <= 0) return emptyList()
        return text.split('\n')
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .takeLast(maxLines)
    }
}
