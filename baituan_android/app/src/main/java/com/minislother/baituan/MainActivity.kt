package com.minislother.baituan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.minislother.baituan.ui.BaituanApp
import com.minislother.baituan.ui.theme.BaituanTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * App 的主 Activity。
 *
 * 这个类只负责创建 Compose 根界面。真正的页面导航、登录判断和业务 UI
 * 都放在 `BaituanApp` 以及各个 Screen/ViewModel 中，避免 Activity 变得臃肿。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * 初始化 Android 主界面，并挂载 Compose 应用内容。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaituanTheme {
                BaituanApp()
            }
        }
    }
}
