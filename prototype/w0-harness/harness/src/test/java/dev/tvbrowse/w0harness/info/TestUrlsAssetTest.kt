package dev.tvbrowse.w0harness.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The bundled, editable assets/test-urls.json (brief §4.1) must parse and every
 * entry must be openable without the harness issuing any request of its own.
 */
class TestUrlsAssetTest {

    private fun locateAsset(): File {
        var dir: File? = File(System.getProperty("user.dir")!!).absoluteFile
        var depth = 0
        while (dir != null && depth < 8) {
            // Works whether the unit-test working directory is the module dir,
            // the Gradle root, or the repository root.
            for (candidate in listOf(
                File(dir, "src/main/assets/test-urls.json"),
                File(dir, "harness/src/main/assets/test-urls.json"),
                File(dir, "prototype/w0-harness/harness/src/main/assets/test-urls.json"),
            )) {
                if (candidate.exists()) return candidate
            }
            dir = dir.parentFile
            depth++
        }
        throw IllegalStateException("test-urls.json not found from ${System.getProperty("user.dir")}")
    }

    @Test
    fun `bundled asset parses and contains valid entries`() {
        val list = TestUrls.parse(locateAsset().readText())
        assertTrue("the bundled list must not be empty", list.urls.isNotEmpty())
        for (entry in list.urls) {
            assertTrue("label must not be blank", entry.label.isNotBlank())
            assertTrue(
                "url must use an explicit scheme (no search fallback): ${entry.url}",
                entry.url.startsWith("http://") || entry.url.startsWith("https://") || entry.url.startsWith("data:"),
            )
        }
    }

    @Test
    fun `parsing tolerates unknown keys`() {
        val list = TestUrls.parse("""{"version":2,"comment":"hi","urls":[{"label":"L","url":"https://example.com/","extra":1}]}""")
        assertEquals(1, list.urls.size)
    }
}
