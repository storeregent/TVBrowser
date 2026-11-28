package dev.tvbrowse.w0harness.web

import android.content.Context
import dev.tvbrowse.w0harness.log.LogIo
import dev.tvbrowse.w0harness.util.jsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Persists the app-owned tab state (brief §5.5) as one JSON file, written on
 * the LogIo thread (D-4: no main-thread file I/O). The external files dir keeps
 * the file adb-visible, which the T-12 protocol and the tools scripts use.
 */
class TabPersistence(context: Context) {

    private val dir: File = context.getExternalFilesDir(null) ?: context.filesDir
    private val file: File = File(dir, FILE_NAME)

    data class Loaded(val payload: String, val tabCount: Int, val activeUrl: String?, val savedAtMs: Long)

    fun saveAsync(manager: TabManager, trigger: String) {
        val payload = manager.toJson()
        val savedAt = System.currentTimeMillis()
        LogIo.execute {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(
                    jsonObject(
                        "savedAtMs" to savedAt,
                        "trigger" to trigger,
                        "payload" to payload,
                    ).toString(),
                )
            }
        }
    }

    fun load(): Loaded? {
        if (!file.exists()) return null
        val parsed = runCatching {
            Json.parseToJsonElement(file.readText()).jsonObject
        }.getOrNull() ?: return null
        val payload = (parsed["payload"] as? JsonPrimitive)?.content ?: return null
        val snapshot = runCatching {
            Json.decodeFromString(TabManager.Snapshot.serializer(), payload)
        }.getOrNull() ?: return null
        val savedAtMs = parsed["savedAtMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        return Loaded(
            payload = payload,
            tabCount = snapshot.tabs.size,
            activeUrl = snapshot.tabs.firstOrNull { it.id == snapshot.activeId }?.currentUrl
                ?: snapshot.tabs.lastOrNull()?.currentUrl,
            savedAtMs = savedAtMs,
        )
    }

    fun clear() {
        LogIo.execute { runCatching { file.delete() } }
    }

    companion object {
        const val FILE_NAME = "tabs-state.json"
    }
}
