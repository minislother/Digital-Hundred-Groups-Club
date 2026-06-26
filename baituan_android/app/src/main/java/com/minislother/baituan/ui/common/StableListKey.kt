package com.minislother.baituan.ui.common

/**
 * 生成 Compose LazyColumn 使用的稳定 key。
 *
 * Compose 要求同一个 LazyColumn 里的 key 不能重复。后端数据如果出现 id 缺失、id 为 0、
 * 或不同区块里名称相同，直接把 id/name 当 key 就可能闪退。这个函数把“区块名 + 下标 + 业务字段”
 * 拼在一起，让 key 在页面内保持唯一，同时仍然足够稳定，方便 Compose 复用列表项。
 */
fun stableListKey(section: String, index: Int, vararg parts: Any?): String {
    val businessPart = parts
        .filterNotNull()
        .joinToString(separator = "-") { it.toString().ifBlank { "blank" } }
        .ifBlank { "empty" }
    return "$section-$index-$businessPart"
}
