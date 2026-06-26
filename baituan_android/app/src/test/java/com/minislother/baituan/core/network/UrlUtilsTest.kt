package com.minislother.baituan.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * UrlUtilsTest 覆盖后端图片和附件路径转完整 URL 的规则。
 *
 * 页面加载社团封面、活动附件时会用到 asBackendUrl()，这里保证空路径、绝对路径和相对路径都安全。
 */
class UrlUtilsTest {

    @Test
    fun testAsBackendUrl_BlankPath_ReturnNull() {
        // Given - 准备空路径
        val value: String? = "   "

        // When - 转换后端资源地址
        val result = value.asBackendUrl("http://localhost:8081/")

        // Then - 空路径不应拼成错误 URL
        assertNull(result)
    }

    @Test
    fun testAsBackendUrl_AbsoluteUrl_ReturnOriginalUrl() {
        // Given - 准备后端已经返回完整地址的场景
        val value = "https://example.com/a.png"

        // When - 转换资源地址
        val result = value.asBackendUrl("http://localhost:8081/")

        // Then - 完整 URL 不需要重复拼接 baseUrl
        assertEquals("https://example.com/a.png", result)
    }

    @Test
    fun testAsBackendUrl_RelativePath_JoinBaseUrl() {
        // Given - 准备没有斜杠和带斜杠的相对路径
        val baseUrl = "http://localhost:8081/"

        // When - 转换两种相对路径
        val noSlash = "uploads/a.png".asBackendUrl(baseUrl)
        val withSlash = "/uploads/a.png".asBackendUrl(baseUrl)

        // Then - 两种写法都应得到同一个可访问地址
        assertEquals("http://localhost:8081/uploads/a.png", noSlash)
        assertEquals("http://localhost:8081/uploads/a.png", withSlash)
    }
}
