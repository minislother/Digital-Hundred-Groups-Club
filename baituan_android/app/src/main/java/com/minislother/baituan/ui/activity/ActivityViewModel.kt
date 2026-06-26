package com.minislother.baituan.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.data.ActivityRepository
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 活动列表页状态。
 *
 * activities 保存当前列表；pageNum/hasNext/loadingMore 控制分页；
 * loadState 用于页面展示加载、错误或报名成功提示。
 */
data class ActivityUiState(
    val searchInfo: String = "",
    val activities: List<ClubActivity> = emptyList(),
    val pageNum: Int = 1,
    val hasNext: Boolean = false,
    val loadingMore: Boolean = false,
    val loadState: LoadState = LoadState()
)

/**
 * 活动列表页 ViewModel。
 *
 * 负责搜索活动、分页加载活动列表，以及提交活动报名请求。
 */
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ActivityUiState())
    val state = _state.asStateFlow()

    /** 更新活动搜索关键字。 */
    fun updateSearch(value: String) {
        _state.update { it.copy(searchInfo = value) }
    }

    /**
     * 加载活动列表。
     *
     * refresh=true 表示刷新第一页；refresh=false 表示继续加载下一页。
     */
    fun load(refresh: Boolean = true) {
        viewModelScope.launch {
            val targetPage = if (refresh) 1 else _state.value.pageNum + 1
            _state.update {
                if (refresh) {
                    it.copy(loadState = LoadState(loading = true))
                } else {
                    it.copy(loadingMore = true, loadState = LoadState())
                }
            }
            runCatching {
                repository.activities(_state.value.searchInfo.ifBlank { null }, targetPage, 10)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        activities = if (refresh) page.items else it.activities + page.items,
                        pageNum = targetPage,
                        hasNext = page.page?.hasNext == true,
                        loadingMore = false,
                        loadState = LoadState()
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loadingMore = false,
                        loadState = LoadState(error = error.message ?: "活动列表加载失败")
                    )
                }
            }
        }
    }

    /** 如果后端还有下一页，就继续加载更多活动。 */
    fun loadNextPage() {
        if (_state.value.hasNext && !_state.value.loadingMore) {
            load(refresh = false)
        }
    }

    /** 提交活动报名申请。 */
    fun join(activityId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                repository.join(activityId)
            }.onSuccess { message ->
                _state.update { it.copy(loadState = LoadState(message = message)) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "报名提交失败"))
                }
            }
        }
    }
}
