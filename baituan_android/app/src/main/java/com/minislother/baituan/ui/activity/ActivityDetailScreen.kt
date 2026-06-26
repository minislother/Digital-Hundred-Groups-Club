package com.minislother.baituan.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.ui.common.EmptyContent
import com.minislother.baituan.ui.common.FullScreenLoading
import com.minislother.baituan.ui.common.InfoCard

/**
 * 活动详情页面。
 *
 * 页面展示活动地点、时间、安排、报名人数等信息，并提供“报名活动”操作。
 * 如果列表传入的信息不完整，会尝试通过活动名和社团名请求后端详情接口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    initialActivity: ClubActivity,
    onBack: () -> Unit,
    viewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = state.activity ?: initialActivity
    val groupName = initialActivity.groupName.orEmpty()

    LaunchedEffect(initialActivity.name, initialActivity.groupName) {
        if (groupName.isNotBlank()) {
            viewModel.load(groupName, initialActivity.name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("活动详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.loadState.loading && state.activity == null) {
            FullScreenLoading("正在加载活动详情")
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
                title = activity.name,
                subtitle = activity.groupName ?: activity.organizer,
                badge = activity.place ?: "活动",
                body = activity.description ?: "暂无活动介绍",
                imageUrl = activity.image,
                imageContentDescription = "${activity.name} 封面"
            )

            DetailLine("所属社团", activity.groupName ?: "暂未填写")
            DetailLine("活动地点", activity.place ?: "暂未填写")
            DetailLine("活动时间", activity.time ?: "暂未填写")
            DetailLine("活动安排", activity.arrange ?: "暂未填写")
            DetailLine("报名人数", activity.number.toString())
            DetailLine("附件", if (activity.attachment.isNullOrBlank()) "暂无附件" else activity.attachment)

            if (state.loadState.error != null || state.loadState.message != null) {
                EmptyContent(state.loadState.error ?: state.loadState.message.orEmpty())
            }

            Button(
                onClick = { viewModel.join(activity.id) },
                enabled = !state.loadState.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.loadState.loading) "提交中" else "报名活动")
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("返回活动列表")
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
