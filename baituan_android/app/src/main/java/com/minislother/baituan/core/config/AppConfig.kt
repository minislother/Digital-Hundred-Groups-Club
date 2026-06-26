package com.minislother.baituan.core.config

import android.content.Context
import com.minislother.baituan.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppConfig 负责集中读取 Android 构建配置。
 *
 * 这里没有使用 Android 自动生成的 BuildConfig，而是从资源文件读取配置。
 * 这样初学者在源码目录中也能看清楚：网络基础地址和日志开关都是由 Gradle
 * 在 debug/release 构建类型中写入资源，再由这个类统一提供给业务代码使用。
 */
@Singleton
class AppConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 后端 API 基础地址，例如模拟器调试时使用 http://10.0.2.2:8081/。 */
    val baseUrl: String = context.getString(R.string.baituan_base_url)

    /** 是否开启网络日志。debug 为 true，release 为 false。 */
    val enableHttpLogging: Boolean = context.resources.getBoolean(R.bool.baituan_enable_http_logging)
}
