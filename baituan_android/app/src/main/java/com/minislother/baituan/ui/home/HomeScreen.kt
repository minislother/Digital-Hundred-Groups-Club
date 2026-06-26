package com.minislother.baituan.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
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
 * 首页 UI。
 *
 * 首页聚合展示热门社团和热门活动。它支持下拉刷新，刷新时会让 HomeViewModel
 * 同时请求两个推荐接口。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    // pullRefresh 提供原生移动端常见的下拉刷新交互。
    val refreshState = rememberPullRefreshState(
        refreshing = state.loadState.loading,
        onRefresh = viewModel::load
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
                // 首页顶部说明，让用户知道这个 App 的核心用途。
                Column {
                    Text(text = "发现校园社团", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "浏览热门社团和近期活动，快速完成申请与报名。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.loadState.loading) {
                item { LoadingContent() }
            }

            if (state.loadState.error != null) {
                item { EmptyContent(state.loadState.error.orEmpty()) }
            }

            // 热门社团区，最多展示前三个推荐社团。
            item { SectionTitle("热门社团") }
            if (state.clubs.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无热门社团") }
            } else {
                itemsIndexed(
                    items = state.clubs.take(3),
                    key = { index, club -> stableListKey("home-club", index, club.id, club.name) }
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
            }

            // 热门活动区，最多展示前三个推荐活动。
            item { SectionTitle("热门活动") }
            if (state.activities.isEmpty() && !state.loadState.loading) {
                item { EmptyContent("暂无热门活动") }
            } else {
                itemsIndexed(
                    items = state.activities.take(3),
                    key = { index, activity -> stableListKey("home-activity", index, activity.id, activity.name) }
                ) { _, activity ->
                    InfoCard(
                        title = activity.name,
                        subtitle = activity.groupName ?: activity.organizer,
                        badge = "报名 ${activity.number}",
                        body = activity.description,
                        imageUrl = activity.image,
                        imageContentDescription = "${activity.name} 封面"
                    )
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
