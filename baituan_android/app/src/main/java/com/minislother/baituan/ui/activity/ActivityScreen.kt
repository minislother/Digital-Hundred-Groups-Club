package com.minislother.baituan.ui.activity

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.ui.common.EmptyContent
import com.minislother.baituan.ui.common.InfoCard
import com.minislother.baituan.ui.common.LoadingContent
import com.minislother.baituan.ui.common.stableListKey

/**
 * 活动列表页面 UI。
 *
 * 展示活动搜索框、活动卡片、报名按钮和加载更多按钮。
 * 页面只负责交互和展示，活动数据由 ActivityViewModel 管理。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ActivityScreen(
    paddingValues: PaddingValues,
    onActivityClick: (ClubActivity) -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(refresh = true)
    }

    // 下拉刷新会重新加载第一页活动数据。
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
                Text(text = "活动", style = MaterialTheme.typography.headlineLarge)
            }
            item {
                // 搜索框只保存关键字；点击“刷新/查询”后请求后端。
                OutlinedTextField(
                    value = state.searchInfo,
                    onValueChange = viewModel::updateSearch,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索活动") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索")
                    },
                    singleLine = true
                )
            }
            item {
                Button(onClick = { viewModel.load(refresh = true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("刷新/查询")
                }
            }

            if (state.loadState.loading) {
                item { LoadingContent() }
            }
            if (state.loadState.error != null) {
                item { EmptyContent(state.loadState.error.orEmpty()) }
            }
            if (state.loadState.message != null) {
                item { EmptyContent(state.loadState.message.orEmpty()) }
            }
            if (state.activities.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无活动数据") }
            }

            // 活动卡片包含活动封面、简介、报名和查看详情两个操作。
            itemsIndexed(
                items = state.activities,
                key = { index, activity -> stableListKey("activity-list", index, activity.id, activity.name) }
            ) { _, activity ->
                InfoCard(
                    title = activity.name,
                    subtitle = activity.groupName ?: activity.organizer,
                    badge = activity.place ?: "活动",
                    body = activity.description,
                    imageUrl = activity.image,
                    imageContentDescription = "${activity.name} 封面",
                    primaryAction = "报名活动",
                    onPrimaryAction = { viewModel.join(activity.id) },
                    secondaryAction = "详情",
                    onSecondaryAction = { onActivityClick(activity) }
                )
            }

            if (state.hasNext) {
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = viewModel::loadNextPage,
                        enabled = !state.loadingMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.loadingMore) "加载中" else "加载更多")
                    }
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
