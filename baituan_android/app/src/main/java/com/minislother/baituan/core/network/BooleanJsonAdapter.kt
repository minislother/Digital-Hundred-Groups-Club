package com.minislother.baituan.core.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * 兼容后端多种布尔值格式的 Gson 适配器。
 *
 * Spring Boot 从数据库返回审核状态时，可能是 true/false，也可能是 0/1，甚至是字符串
 * "0"/"1"。如果只使用 Gson 默认 Boolean 解析，遇到数字时可能抛异常，导致整个列表加载失败。
 */
class BooleanJsonAdapter : JsonDeserializer<Boolean?> {
    /**
     * 将后端返回的多种布尔值表达转换为 Kotlin Boolean。
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean? {
        if (json == null || json.isJsonNull) {
            return null
        }

        val primitive = json.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asInt != 0
            primitive.isString -> parseStringBoolean(primitive.asString)
            else -> null
        }
    }

    /** 把字符串形式的 true/false、1/0、yes/no 都归一成 Kotlin Boolean。 */
    private fun parseStringBoolean(value: String): Boolean? {
        return when (value.trim().lowercase()) {
            "true", "1", "yes", "y" -> true
            "false", "0", "no", "n" -> false
            "" -> null
            else -> null
        }
    }
}
