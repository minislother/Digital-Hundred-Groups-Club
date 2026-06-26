package com.minislother.baituan.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.ui.activity.ActivityDetailScreen
import com.minislother.baituan.ui.activity.ActivityScreen
import com.minislother.baituan.ui.admin.AdminDashboardScreen
import com.minislother.baituan.ui.club.ClubDetailScreen
import com.minislother.baituan.ui.club.ClubScreen
import com.minislother.baituan.ui.home.HomeScreen
import com.minislother.baituan.ui.profile.ProfileScreen

/**
 * 登录后的主界面分发器。
 *
 * 学生端仍然使用原来的四个底部 Tab；社团管理员和超级管理员进入管理工作台。
 * 把分发逻辑放在这里，可以保证根导航只关心“登录/未登录”，不关心每个端内部怎么组织页面。
 */
@Composable
fun MainScreen(role: UserRole) {
    when (role) {
        UserRole.Student -> StudentMainScreen()
        UserRole.Manager,
        UserRole.SuperAdmin -> AdminDashboardScreen(role = role)
    }
}

/**
 * 底部导航栏的配置项。
 *
 * label 是用户看到的文字，icon 是图标，contentDescription 用于无障碍朗读。
 */
private data class TabItem(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

/** 学生端主界面的四个一级页面。 */
private val studentTabs = listOf(
    TabItem("首页", Icons.Outlined.Home, "首页"),
    TabItem("社团", Icons.Outlined.Groups, "社团列表"),
    TabItem("活动", Icons.Outlined.Celebration, "活动列表"),
    TabItem("我的", Icons.Outlined.AccountCircle, "我的信息")
)

/**
 * 学生端登录后的主界面。
 *
 * 社团详情和活动详情不是单独 Tab，而是通过 selectedClub/selectedActivity 临时进入详情页；
 * 返回时再回到原列表，移动端使用起来更接近常见 App 的层级导航。
 */
@Composable
private fun StudentMainScreen() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedClub by remember { mutableStateOf<Club?>(null) }
    var selectedActivity by remember { mutableStateOf<ClubActivity?>(null) }

    selectedClub?.let { club ->
        ClubDetailScreen(
            initialClub = club,
            onBack = { selectedClub = null }
        )
        return
    }

    selectedActivity?.let { activity ->
        ActivityDetailScreen(
            initialActivity = activity,
            onBack = { selectedActivity = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                studentTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(tab.icon, contentDescription = tab.contentDescription)
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedIndex) {
            0 -> HomeScreen(paddingValues)
            1 -> ClubScreen(
                paddingValues = paddingValues,
                onClubClick = { selectedClub = it }
            )
            2 -> ActivityScreen(
                paddingValues = paddingValues,
                onActivityClick = { selectedActivity = it }
            )
            else -> ProfileScreen(paddingValues)
        }
    }
}
