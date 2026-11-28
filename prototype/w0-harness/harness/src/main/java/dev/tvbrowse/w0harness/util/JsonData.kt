package dev.tvbrowse.w0harness.util

import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Small helpers to build kotlinx-serialization JSON payloads for log lines from
 * plain Kotlin values. Pure JVM code (unit-testable).
 */
fun jsonObject(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((key, value) in pairs) putValue(key, value)
}

fun jsonObject(pairs: Iterable<Pair<String, Any?>>): JsonObject = buildJsonObject {
    for ((key, value) in pairs) putValue(key, value)
}

fun JsonObjectBuilder.putValue(key: String, value: Any?) {
    when (value) {
        null -> put(key, JsonNull)
        is String -> put(key, value)
        is Int -> put(key, value)
        is Long -> put(key, value)
        is Boolean -> put(key, value)
        is Float -> put(key, JsonPrimitive(value))
        is Double -> put(key, JsonPrimitive(value))
        is JsonObject -> put(key, value)
        is JsonElement -> put(key, value)
        is Map<*, *> -> put(key, jsonObject(value.map { (k, v) -> k.toString() to v }))
        is Iterable<*> -> putJsonArray(key) {
            for (element in value) addValue(element)
        }
        else -> put(key, value.toString())
    }
}

private fun JsonArrayBuilder.addValue(value: Any?) {
    when (value) {
        null -> add(JsonNull)
        is JsonElement -> add(value)
        is String -> add(JsonPrimitive(value))
        is Int -> add(JsonPrimitive(value))
        is Long -> add(JsonPrimitive(value))
        is Boolean -> add(JsonPrimitive(value))
        is Float -> add(JsonPrimitive(value))
        is Double -> add(JsonPrimitive(value))
        else -> add(JsonPrimitive(value.toString()))
    }
}
