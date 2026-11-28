package dev.tvbrowse.w0harness.util

/**
 * Diff helper for `onConfigurationChanged` logging (brief §5.2).
 *
 * Android side extracts the interesting `Configuration` fields into a plain map
 * (see [dev.tvbrowse.w0harness.util.ConfigurationFields]); the diff itself is pure
 * JVM logic so it can be unit-tested.
 */
object ConfigurationDiff {

    /** Returns the fields whose value changed, as `field to (oldValue, newValue)`. */
    fun changedFields(old: Map<String, String>, new: Map<String, String>): Map<String, Pair<String, String>> {
        val keys = (old.keys + new.keys).toSortedSet()
        val result = linkedMapOf<String, Pair<String, String>>()
        for (key in keys) {
            val o = old[key] ?: "<absent>"
            val n = new[key] ?: "<absent>"
            if (o != n) result[key] = o to n
        }
        return result
    }
}
