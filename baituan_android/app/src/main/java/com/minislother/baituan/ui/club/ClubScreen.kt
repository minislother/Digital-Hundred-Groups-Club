package com.minislother.baituan.ui.club

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
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.ui.common.EmptyContent
import com.minislother.baituan.ui.common.InfoCard
import com.minislother.baituan.ui.common.LoadingContent
import com.minislother.baituan.ui.common.stableListKey

/**
 * 社团列表页面 UI。
 *
 * 这个页面负责展示社团搜索框、社团卡片、申请加入按钮和加载更多按钮。
 * 真正的数据加载和申请操作由 ClubViewModel 负责。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ClubScreen(
    paddingValues: PaddingValues,
    onClubClick: (Club) -> Unit,
    viewModel: ClubViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(refresh = true)
    }

    // 下拉刷新会重新加载第一页社团数据。
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
                Text(text = "社团", style = MaterialTheme.typography.headlineLarge)
            }
            item {
                // 搜索框只修改 ViewModel 中的 searchInfo；点击按钮后才真正请求后端。
                OutlinedTextField(
                    value = state.searchInfo,
                    onValueChange = viewModel::updateSearch,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索社团") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索")
                    },
                    singleLine = true
                )
            }

            item {
                androidx.compose.material3.Button(
                    onClick = { viewModel.load(refresh = true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
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
            if (state.clubs.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无社团数据") }
            }

            // 社团卡片包含封面、介绍、申请加入和查看详情两个操作。
            itemsIndexed(
                items = state.clubs,
                key = { index, club -> stableListKey("club-list", index, club.id, club.name) }
            ) { _, club ->
                InfoCard(
                    title = club.name,
                    subtitle = club.leader?.let { "负责人：$it" },
                    badge = "热度 ${club.hot}",
                    body = club.description,
                    imageUrl = club.image,
                    imageContentDescription = "${club.name} 封面",
                    primaryAction = "申请加入",
                    onPrimaryAction = { viewModel.applyJoin(club.id) },
                    secondaryAction = "详情",
                    onSecondaryAction = { onClubClick(club) }
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
