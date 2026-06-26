package com.minislother.baituan.core.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JsonParsingTest 覆盖 Repository 读取 data 对象时用到的通用 JSON 工具。
 *
 * 这些工具负责把后端 data.items、data.page、data.token 等字段转成 Kotlin 对象。
 */
class JsonParsingTest {

    private data class DemoItem(val id: Int = 0, val name: String = "")

    private val gson = Gson()

    @Test
    fun testReadList_ArrayContainsNull_FilterNullItems() {
        // Given - 准备包含 null 元素的后端列表
        val data = JsonParser.parseString(
            """{"items":[{"id":1,"name":"摄影社"},null,{"id":2,"name":"篮球社"}]}"""
        ).asJsonObject

        // When - 读取列表字段
        val result = data.readList<DemoItem>(gson, "items")

        // Then - null 元素会被过滤，避免列表渲染时崩溃
        assertEquals(2, result.size)
        assertEquals("摄影社", result[0].name)
        assertEquals("篮球社", result[1].name)
    }

    @Test
    fun testReadObject_MissingKey_ReturnNull() {
        // Given - 准备不包含目标对象的 data
        val data = JsonParser.parseString("""{"other":{}}""").asJsonObject

        // When - 读取不存在的对象字段
        val result = data.readObject<DemoItem>(gson, "item")

        // Then - 不存在时返回 null，由调用方决定是否展示空状态
        assertNull(result)
    }

    @Test
    fun testReadString_TokenField_ReturnString() {
        // Given - 准备包含 token 的 data
        val data = JsonParser.parseString("""{"token":"abc.def.ghi"}""").asJsonObject

        // When - 读取字符串字段
        val result = data.readString("token")

        // Then - 返回原始 token 字符串
        assertEquals("abc.def.ghi", result)
    }

    @Test
    fun testReadPage_PageObject_ReturnPageInfo() {
        // Given - 准备后端分页结构
        val data = JsonParser.parseString(
            """{"page":{"total":12,"pageNum":2,"pageSize":5,"pages":3,"hasPrevious":true,"hasNext":true}}"""
        ).asJsonObject

        // When - 读取分页对象
        val result = data.readPage(gson)

        // Then - 分页字段应完整转换
        assertEquals(12, result?.total)
        assertEquals(2, result?.pageNum)
        assertEquals(true, result?.hasNext)
    }
}
