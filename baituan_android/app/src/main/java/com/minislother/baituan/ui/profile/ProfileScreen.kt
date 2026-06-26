package com.minislother.baituan.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minislother.baituan.ui.common.EmptyContent
import com.minislother.baituan.ui.common.InfoCard
import com.minislother.baituan.ui.common.LoadingContent
import com.minislother.baituan.ui.common.SectionTitle
import com.minislother.baituan.ui.common.stableListKey

/**
 * “我的”页面 UI。
 *
 * 这里集中展示当前学生的个人资料、我的社团、我的活动和我的入团申请。
 * 因为数据来源较多，所以页面只负责排列内容，加载逻辑放在 ProfileViewModel。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(refresh = true)
    }

    // 下拉刷新会重新加载个人资料、社团、活动和第一页申请记录。
    val refreshState = rememberPullRefreshState(
        refreshing = state.loadState.loading,
        onRefresh = { viewModel.load(refresh = true) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pullRefresh(refreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "我的", style = MaterialTheme.typography.headlineLarge)
            }

            if (state.loadState.loading) {
                item { LoadingContent() }
            }
            if (state.loadState.error != null) {
                item { EmptyContent(state.loadState.error.orEmpty()) }
            }

            // 个人资料卡片，优先展示昵称，没有昵称时展示姓名或学号。
            state.student?.let { student ->
                item {
                    InfoCard(
                        title = student.nickname ?: student.userName ?: student.userId,
                        subtitle = student.major ?: student.school,
                        badge = student.userId,
                        body = student.description ?: "暂未填写个人简介"
                    )
                }
            }

            // 我的社团：来自 /individualGroup/allGroups。
            item { SectionTitle("我的社团") }

            if (state.clubs.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无已加入或申请中的社团") }
            }

            itemsIndexed(
                items = state.clubs.take(5),
                key = { index, club -> stableListKey("profile-club", index, club.id, club.name) }
            ) { _, club ->
                InfoCard(
                    title = club.name,
                    subtitle = club.leader?.let { "负责人：$it" },
                    badge = "热度 ${club.hot}",
                    body = club.description,
                    imageUrl = club.image,
                    imageContentDescription = "${club.name} 封面"
                )
            }

            // 我的活动：当前后端返回的是活动关系记录，所以先展示活动编号和审核状态。
            item { SectionTitle("我的活动") }

            if (state.activities.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无已报名或申请中的活动") }
            }

            itemsIndexed(
                items = state.activities,
                key = { index, relation ->
                    stableListKey("profile-activity", index, relation.id, relation.activityId, relation.studentId)
                }
            ) { _, relation ->
                val status = when {
                    relation.isAccepted == true || relation.status == 1 -> "已通过"
                    relation.isAccepted == false || relation.status < 0 -> "未通过"
                    else -> "待审核"
                }
                InfoCard(
                    title = "活动 #${relation.activityId}",
                    subtitle = relation.position ?: "成员",
                    badge = status,
                    body = relation.createTime?.let { "申请时间：$it" }
                )
            }

            // 我的入团申请：来自 /application/myApps，支持继续加载下一页。
            item { SectionTitle("我的入团申请") }

            if (state.applications.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无申请记录") }
            }

            itemsIndexed(
                items = state.applications,
                key = { index, application ->
                    stableListKey("profile-application", index, application.applicationId, application.groupName)
                }
            ) { _, application ->
                val status = when (application.isAccepted) {
                    true -> "已通过"
                    false -> "未通过"
                    null -> "待审核"
                }
                InfoCard(
                    title = application.groupName,
                    subtitle = application.createTime,
                    badge = status,
                    body = application.selfIntro
                )
            }

            item {
                Button(onClick = { viewModel.load(refresh = true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("刷新")
                }
            }

            if (state.hasMoreApplications) {
                item {
                    OutlinedButton(
                        onClick = viewModel::loadMoreApplications,
                        enabled = !state.loadingMoreApplications,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.loadingMoreApplications) "加载中" else "加载更多申请")
                    }
                }
            }

            item {
                OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                    Text("退出登录")
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.loadState.loading,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
