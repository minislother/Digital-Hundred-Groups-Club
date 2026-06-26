package com.minislother.baituan.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ApiEnvelopeTest 覆盖后端统一响应的成功和失败判断。
 *
 * Android Repository 会先调用 requireSuccess()，所以这里重点验证成功响应能继续向下处理，
 * 失败响应会转换成页面可感知的 ApiException。
 */
class ApiEnvelopeTest {

    @Test
    fun testRequireSuccess_Success_ReturnOriginalEnvelope() {
        // Given - 准备一个后端成功响应
        val envelope = ApiEnvelope(success = true, code = 20000, message = "ok")

        // When - 执行统一成功校验
        val result = envelope.requireSuccess()

        // Then - 成功响应不应被重新包装，方便调用方继续读取 data
        assertSame(envelope, result)
    }

    @Test
    fun testRequireSuccess_FailedResponse_ThrowApiExceptionWithBackendMessage() {
        // Given - 准备一个带明确错误消息的失败响应
        val envelope = ApiEnvelope(success = false, code = 50000, message = "登录已过期")

        // When & Then - 失败响应会抛出 ApiException，并保留后端提示
        val exception = assertThrows(ApiException::class.java) {
            envelope.requireSuccess()
        }
        assertEquals("登录已过期", exception.message)
    }

    @Test
    fun testRequireSuccess_BlankMessage_ThrowApiExceptionWithDefaultMessage() {
        // Given - 准备一个没有 message 的失败响应
        val envelope = ApiEnvelope(success = false, code = 50000, message = "")

        // When & Then - 默认提示不能是空，避免页面展示空白错误
        val exception = assertThrows(ApiException::class.java) {
            envelope.requireSuccess()
        }
        assertTrue(exception.message.orEmpty().isNotBlank())
    }
}
