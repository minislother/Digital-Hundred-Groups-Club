package com.minislother.baituan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.ui.auth.LoginScreen
import com.minislother.baituan.ui.auth.RegisterScreen
import com.minislother.baituan.ui.auth.RoleSelectionScreen
import com.minislother.baituan.ui.main.MainScreen
import com.minislother.baituan.ui.session.SessionViewModel

private object Routes {
    const val RoleSelect = "role_select"
    const val Login = "login/{role}"
    const val Register = "register"
    const val Main = "main"

    /**
     * 处理当前模块的业务状态或界面事件。
     */
    fun login(role: UserRole): String = "login/${role.storageValue}"
}

/**
 * App 的根导航组件。
 *
 * 这里统一决定用户该看到哪个大入口：未登录时先选身份，选择后进入对应登录页；
 * 登录成功后根据保存的身份进入学生端、社团管理员端或超级管理员端。
 */
@Composable
fun BaituanApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    LaunchedEffect(session.isLoggedIn) {
        val target = if (session.isLoggedIn) Routes.Main else Routes.RoleSelect
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.RoleSelect) {
        composable(Routes.RoleSelect) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    navController.navigate(Routes.login(role)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = Routes.Login,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = UserRole.fromStorageValue(backStackEntry.arguments?.getString("role")) ?: UserRole.Student
            LoginScreen(
                role = role,
                onBackToRoleSelect = {
                    navController.navigate(Routes.RoleSelect) {
                        popUpTo(Routes.RoleSelect) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onRegisterClick = {
                    navController.navigate(Routes.Register) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                onBackToLogin = {
                    navController.navigate(Routes.login(UserRole.Student)) {
                        popUpTo(Routes.Register) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.Main) {
            MainScreen(role = session.role ?: UserRole.Student)
        }
    }
}
