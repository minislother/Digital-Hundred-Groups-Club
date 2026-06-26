package com.minislother.baituan.ui.common

/**
 * 页面通用加载状态。
 *
 * loading 表示正在请求接口；error 表示失败提示；message 表示成功或普通提示。
 * 多个页面复用这个状态，可以减少重复代码。
 */
data class LoadState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
