package com.minislother.baituan.core.network

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BooleanJsonAdapterTest 验证后端不同布尔值格式都能被 Android 正确解析。
 *
 * 后端可能返回 true/false、1/0、"yes"/"no" 等不同格式；如果解析失败，列表页可能加载失败。
 */
class BooleanJsonAdapterTest {

    private data class BooleanHolder(val enabled: Boolean?)

    private val gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.javaObjectType, BooleanJsonAdapter())
        .create()

    @Test
    fun testDeserialize_StandardBoolean_ReturnBoolean() {
        // Given - 准备标准 JSON 布尔值
        val json = """{"enabled":true}"""

        // When - 通过注册了适配器的 Gson 解析
        val result = gson.fromJson(json, BooleanHolder::class.java)

        // Then - true 应被正常解析
        assertTrue(result.enabled == true)
    }

    @Test
    fun testDeserialize_NumberBoolean_ReturnBoolean() {
        // Given - 准备后端常见的数字状态值
        val enabledJson = """{"enabled":1}"""
        val disabledJson = """{"enabled":0}"""

        // When - 分别解析数字状态
        val enabled = gson.fromJson(enabledJson, BooleanHolder::class.java)
        val disabled = gson.fromJson(disabledJson, BooleanHolder::class.java)

        // Then - 非 0 为 true，0 为 false
        assertEquals(true, enabled.enabled)
        assertEquals(false, disabled.enabled)
    }

    @Test
    fun testDeserialize_StringYesNo_ReturnBoolean() {
        // Given - 准备字符串形式的布尔状态
        val yesJson = """{"enabled":"yes"}"""
        val noJson = """{"enabled":"no"}"""

        // When - 解析字符串布尔状态
        val yes = gson.fromJson(yesJson, BooleanHolder::class.java)
        val no = gson.fromJson(noJson, BooleanHolder::class.java)

        // Then - yes/y 视为 true，no/n 视为 false
        assertTrue(yes.enabled == true)
        assertFalse(no.enabled == true)
    }

    @Test
    fun testDeserialize_NullAndUnknownString_ReturnNull() {
        // Given - 准备空值和无法识别的字符串
        val nullJson = """{"enabled":null}"""
        val unknownJson = """{"enabled":"unknown"}"""

        // When - 执行解析
        val nullResult = gson.fromJson(nullJson, BooleanHolder::class.java)
        val unknownResult = gson.fromJson(unknownJson, BooleanHolder::class.java)

        // Then - 无法确认含义时返回 null，由业务层决定如何处理
        assertNull(nullResult.enabled)
        assertNull(unknownResult.enabled)
    }
}
