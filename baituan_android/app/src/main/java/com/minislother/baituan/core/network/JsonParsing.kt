package com.minislother.baituan.core.network

import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Reads one object from the backend data object.
 *
 * For example, when the club detail API returns data.group, callers can use
 * readObject<Club>(gson, "group") to convert that JSON object into a Club model.
 */
inline fun <reified T : Any> JsonObject?.readObject(gson: Gson, key: String): T? {
    val element = this?.get(key)?.takeIf { !it.isJsonNull } ?: return null
    return gson.fromJson(element, T::class.java)
}

/**
 * Reads a list from the backend data object.
 *
 * Some backend endpoints may return null or malformed entries inside arrays.
 * Each item is parsed separately so one bad row cannot crash the whole Compose screen.
 */
inline fun <reified T : Any> JsonObject?.readList(gson: Gson, key: String): List<T> {
    val array = this?.get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
    return array.mapNotNull { item ->
        if (item == null || item.isJsonNull) {
            null
        } else {
            runCatching { gson.fromJson(item, T::class.java) }.getOrNull()
        }
    }
}

/** Reads a string field from data, such as the login token. */
fun JsonObject?.readString(key: String): String? {
    return this?.get(key)?.takeIf { !it.isJsonNull }?.asString
}

/** Reads the common data.page pagination object. */
fun JsonObject?.readPage(gson: Gson): PageInfo? {
    return readObject(gson, "page")
}
