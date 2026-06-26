package com.minislother.baituan.core.network

/**
 * 把后端返回的图片/附件路径转换成完整 URL。
 *
 * 后端有时会返回完整的 http/https 地址，有时可能只返回相对路径。
 * 这个工具函数统一处理两种情况，保证 Coil 图片加载器拿到的是可访问地址。
 */
fun String?.asBackendUrl(baseUrl: String): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

    val base = baseUrl.trimEnd('/')
    return if (raw.startsWith("/")) {
        "$base$raw"
    } else {
        "$base/$raw"
    }
}
