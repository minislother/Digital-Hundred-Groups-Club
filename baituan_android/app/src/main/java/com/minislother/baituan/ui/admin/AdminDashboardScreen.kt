package com.minislother.baituan.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.domain.model.ClubRelation
import com.minislother.baituan.domain.model.PlatformUser
import com.minislother.baituan.domain.model.RankingStat
import com.minislother.baituan.ui.common.EmptyContent
import com.minislother.baituan.ui.common.LoadingContent
import com.minislother.baituan.ui.common.SectionTitle
import com.minislother.baituan.ui.common.stableListKey

/**
 * 社团管理员端和超级管理员端的工作台。
 *
 * 两个管理端都属于“管理工作流”，所以复用同一个 Scaffold、顶部栏和刷新/退出操作；
 * 中间内容根据 role 切换，避免重复写两套相似页面。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminDashboardScreen(
    role: UserRole,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(role) {
        viewModel.load(role)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = role.displayName)
                        Text(
                            text = "数字百团移动工作台",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(role) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新数据")
                    }
                    IconButton(onClick = { viewModel.logout(role) }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "退出登录")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.loadState.loading) {
                item { LoadingContent("正在加载管理数据") }
            }
            if (state.loadState.error != null) {
                item { EmptyContent(state.loadState.error.orEmpty()) }
            }

            when (role) {
                UserRole.Manager -> managerContent(state)
                UserRole.SuperAdmin -> superAdminContent(state)
                UserRole.Student -> item { EmptyContent("学生端请返回学生首页使用") }
            }
        }
    }
}

/** 社团管理员端内容：重点展示自己负责的社团、活动和待审核申请。 */
private fun androidx.compose.foundation.lazy.LazyListScope.managerContent(state: AdminDashboardState) {
    item {
        MetricRow(
            firstTitle = "管理社团",
            firstValue = state.managedClubs.size.toString(),
            secondTitle = "管理活动",
            secondValue = state.managedActivities.size.toString()
        )
    }
    item {
        MetricRow(
            firstTitle = "待审入团",
            firstValue = state.pendingClubRelations.size.toString(),
            secondTitle = "今日关注",
            secondValue = if (state.pendingClubRelations.isEmpty()) "正常" else "待处理"
        )
    }

    item { SectionTitle("我管理的社团") }
    if (state.managedClubs.isEmpty() && !state.loadState.loading) {
        item { EmptyContent("暂无负责的社团") }
    } else {
        itemsIndexed(
            items = state.managedClubs.take(6),
            key = { index, club -> stableListKey("managed-club", index, club.id, club.name) }
        ) { _, club ->
            ClubSummaryCard(club)
        }
    }

    item { SectionTitle("我管理的活动") }
    if (state.managedActivities.isEmpty() && !state.loadState.loading) {
        item { EmptyContent("暂无负责的活动") }
    } else {
        itemsIndexed(
            items = state.managedActivities.take(6),
            key = { index, activity -> stableListKey("managed-activity", index, activity.id, activity.name) }
        ) { _, activity ->
            ActivitySummaryCard(activity)
        }
    }

    item { SectionTitle("待审核入团申请") }
    if (state.pendingClubRelations.isEmpty() && !state.loadState.loading) {
        item { EmptyContent("当前没有待审核入团申请") }
    } else {
        itemsIndexed(
            items = state.pendingClubRelations.take(8),
            key = { index, relation ->
                stableListKey("club-relation", index, relation.id, relation.groupId, relation.studentId)
            }
        ) { _, relation ->
            ClubRelationCard(relation)
        }
    }
}

/** 超级管理员端内容：重点展示平台总览、审批队列和排行榜。 */
private fun androidx.compose.foundation.lazy.LazyListScope.superAdminContent(state: AdminDashboardState) {
    item {
        MetricRow(
            firstTitle = "用户",
            firstValue = state.users.size.toString(),
            secondTitle = "社团",
            secondValue = state.allClubs.size.toString()
        )
    }
    item {
        MetricRow(
            firstTitle = "活动",
            firstValue = state.allActivities.size.toString(),
            secondTitle = "待审事项",
            secondValue = (state.clubApplications.size + state.activityApplications.size).toString()
        )
    }

    item { SectionTitle("用户概览") }
    if (state.users.isEmpty() && !state.loadState.loading) {
        item { EmptyContent("暂无用户数据") }
    } else {
        itemsIndexed(
            items = state.users.take(8),
            key = { index, user -> stableListKey("platform-user", index, user.userId, user.status) }
        ) { _, user ->
            UserSummaryCard(user)
        }
    }

    item { SectionTitle("社团审批") }
    if (state.clubApplications.isEmpty() && !state.loadState.loading) {
        item { EmptyContent("暂无社团审批记录") }
    } else {
        itemsIndexed(
            items = state.clubApplications.take(5),
            key = { index, club -> stableListKey("club-app", index, club.id, club.name) }
        ) { _, club ->
            ClubSummaryCard(club)
        }
    }

    item { SectionTitle("活动审批") }
    if (state.activityApplications.isEmpty() && !state.loadState.loading) {
        item { EmptyContent("暂无活动审批记录") }
    } else {
        itemsIndexed(
            items = state.activityApplications.take(5),
            key = { index, activity -> stableListKey("activity-app", index, activity.id, activity.name) }
        ) { _, activity ->
            ActivitySummaryCard(activity)
        }
    }

    item { SectionTitle("社团人数排行") }
    rankingItems("group-ranking", state.groupStats)

    item { SectionTitle("活动人数排行") }
    rankingItems("activity-ranking", state.activityStats)
}

/** 两列指标卡，用于移动端快速扫读关键数量。 */
@Composable
private fun MetricRow(
    firstTitle: String,
    firstValue: String,
    secondTitle: String,
    secondValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(firstTitle, firstValue, Modifier.weight(1f))
        MetricCard(secondTitle, secondValue, Modifier.weight(1f))
    }
}

/** 单个指标卡。 */
@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** 社团摘要卡。 */
@Composable
private fun ClubSummaryCard(club: Club) {
    SimpleListCard(
        title = club.name.ifBlank { "未命名社团" },
        subtitle = "负责人：${club.leader ?: "-"}",
        trailing = statusText(club.isAccepted),
        body = club.description
    )
}

/** 活动摘要卡。 */
@Composable
private fun ActivitySummaryCard(activity: ClubActivity) {
    SimpleListCard(
        title = activity.name.ifBlank { "未命名活动" },
        subtitle = activity.groupName ?: activity.organizer ?: "未填写组织者",
        trailing = statusText(activity.isAccepted),
        body = activity.place ?: activity.time
    )
}

/** 入团申请摘要卡。 */
@Composable
private fun ClubRelationCard(relation: ClubRelation) {
    SimpleListCard(
        title = relation.userName ?: relation.studentId ?: "未知学生",
        subtitle = "学号：${relation.studentId ?: "-"}",
        trailing = "状态 ${relation.status}",
        body = relation.position
    )
}

/** 用户摘要卡。 */
@Composable
private fun UserSummaryCard(user: PlatformUser) {
    SimpleListCard(
        title = user.userName ?: user.nickname ?: user.userId,
        subtitle = "${user.roleName} · ${user.userId}",
        trailing = "状态 ${user.status}",
        body = user.email ?: user.phone ?: user.major
    )
}

/** 通用列表卡片，减少管理端各类摘要 UI 的重复代码。 */
@Composable
private fun SimpleListCard(
    title: String,
    subtitle: String,
    trailing: String?,
    body: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!trailing.isNullOrBlank()) {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 排行列表，最多展示前五项，避免移动端首屏信息过载。 */
private fun androidx.compose.foundation.lazy.LazyListScope.rankingItems(section: String, items: List<RankingStat>) {
    if (items.isEmpty()) {
        item { EmptyContent("暂无排行数据") }
    } else {
        itemsIndexed(
            items = items.take(5),
            key = { index, stat -> stableListKey(section, index, stat.id, stat.activityId, stat.groupId, stat.displayName) }
        ) { _, stat ->
            SimpleListCard(
                title = stat.displayName,
                subtitle = "成员或报名人数",
                trailing = "${stat.displayCount}",
                body = null
            )
        }
    }
}

/** 把后端审核布尔值转换成页面展示文案。 */
private fun statusText(value: Boolean?): String {
    return when (value) {
        true -> "已通过"
        false -> "未通过"
        null -> "待审核"
    }
}
