package com.minislother.baituan.core.network

import com.google.gson.JsonObject

/**
 * 后端统一响应结构。
 *
 * Spring Boot 后端大多数接口都会返回：
 * - success：是否成功
 * - code：业务状态码，成功通常是 20000
 * - message：提示信息
 * - data：真正的数据内容
 */
data class ApiEnvelope(
    val success: Boolean = false,
    val code: Int = 0,
    val message: String = "",
    val data: JsonObject? = null
)

/**
 * 分页信息。
 *
 * 后端分页接口会把列表放在 `data.items`，分页元信息放在 `data.page`。
 * Android 端通过 hasNext 判断是否还能继续加载下一页。
 */
data class PageInfo(
    val total: Int = 0,
    val pageNum: Int = 1,
    val pageSize: Int = 10,
    val pages: Int = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false
)

/** 网络或业务响应失败时抛出的异常，ViewModel 会把它转换成页面错误提示。 */
class ApiException(message: String) : Exception(message)

/**
 * 检查后端响应是否成功。
 *
 * 这样 Repository 层不用每次都手写 `success && code == 20000`，
 * 调用 `requireSuccess()` 后，要么拿到成功响应，要么直接进入错误处理流程。
 */
fun ApiEnvelope.requireSuccess(): ApiEnvelope {
    if (success && code == 20000) {
        return this
    }
    throw ApiException(message.ifBlank { "请求失败，请稍后再试" })
}
