package com.minislother.baituan

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Android 应用入口类。
 *
 * `@HiltAndroidApp` 会启动 Hilt 依赖注入框架，让 ViewModel、Repository、
 * 网络模块等对象可以通过构造函数自动获得自己需要的依赖。
 */
@HiltAndroidApp
class BaituanApplication : Application()
