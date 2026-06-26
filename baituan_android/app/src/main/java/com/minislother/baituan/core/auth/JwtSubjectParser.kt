package com.minislother.baituan.core.auth

import android.util.Base64
import com.google.gson.JsonParser

/**
 * JWT subject 解析工具。
 *
 * 后端生成 token 时，会把当前学生学号放到 JWT 的 subject/sub 字段里。
 * Android 端读取“我的资料/我的申请”时需要知道当前学号，所以这里提供一个轻量解析方法。
 *
 * 注意：这里不是做安全校验，真正的 token 是否有效仍然由后端验证。
 */
object JwtSubjectParser {
    /** 从 JWT token 中解析 sub 字段，解析失败时返回 null。 */
    fun subject(token: String?): String? {
        if (token.isNullOrBlank()) return null
        val payload = token.split(".").getOrNull(1) ?: return null
        return runCatching {
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JsonParser.parseString(String(decoded)).asJsonObject
            json.get("sub")?.asString
        }.getOrNull()
    }
}
