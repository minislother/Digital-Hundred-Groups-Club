package com.minislother.baituan.ui.club

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.ui.common.EmptyContent
import com.minislother.baituan.ui.common.FullScreenLoading
import com.minislother.baituan.ui.common.InfoCard

/**
 * 社团详情页面。
 *
 * 列表页会先把已知的社团对象传进来，页面打开后再请求后端详情接口刷新完整数据。
 * 这样用户点击后能尽快看到已有信息，同时还能拿到最新详情。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailScreen(
    initialClub: Club,
    onBack: () -> Unit,
    viewModel: ClubDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val club = state.club ?: initialClub

    LaunchedEffect(initialClub.name) {
        viewModel.load(initialClub.name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("社团详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
)
        }
    ) { paddingValues ->
        if (state.loadState.loading && state.club == null) {
            FullScreenLoading("正在加载社团详情")
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                title = club.name,
                subtitle = club.leader?.let { "负责人：$it" },
                badge = "热度 ${club.hot}",
                body = club.description ?: "暂无社团介绍",
                imageUrl = club.image,
                imageContentDescription = "${club.name} 封面"
            )

            DetailLine("社团编号", club.id.toString())
            DetailLine("负责人", club.leader ?: "暂未填写")
            DetailLine("附件", if (club.attachment.isNullOrBlank()) "暂无附件" else club.attachment)

            if (state.loadState.error != null || state.loadState.message != null) {
                EmptyContent(state.loadState.error ?: state.loadState.message.orEmpty())
            }

            Button(
                onClick = { viewModel.applyJoin(club.id) },
                enabled = !state.loadState.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.loadState.loading) "提交中" else "申请加入")
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("返回社团列表")
            }
        }
    }
}

/**
 * 处理当前模块的业务状态或界面事件。
 */
@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
