package dev.tvbrowse.w0harness.info

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bundled, editable test URL list (brief §4.1): assets/test-urls.json.
 * Parsing is pure JVM logic (unit-tested against the bundled asset).
 */
@Serializable
data class TestUrl(val label: String, val url: String, val note: String? = null)

@Serializable
data class TestUrlList(val version: Int = 1, val urls: List<TestUrl> = emptyList())

object TestUrls {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): TestUrlList = json.decodeFromString(TestUrlList.serializer(), text)

    fun load(context: Context): TestUrlList = runCatching {
        parse(context.assets.open("test-urls.json").bufferedReader().use { it.readText() })
    }.getOrDefault(TestUrlList())
}
