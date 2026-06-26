package com.minislother.baituan.ui.club

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.data.ClubRepository
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 社团列表页状态。
 *
 * clubs 保存当前已经加载出的社团；pageNum/hasNext/loadingMore 用于分页加载；
 * loadState 用于展示首屏加载、错误和申请成功提示。
 */
data class ClubUiState(
    val searchInfo: String = "",
    val clubs: List<Club> = emptyList(),
    val pageNum: Int = 1,
    val hasNext: Boolean = false,
    val loadingMore: Boolean = false,
    val loadState: LoadState = LoadState()
)

/**
 * 社团列表页 ViewModel。
 *
 * 负责搜索、分页加载社团，以及提交“申请加入社团”的操作。
 */
@HiltViewModel
class ClubViewModel @Inject constructor(
    private val repository: ClubRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ClubUiState())
    val state = _state.asStateFlow()

    /** 更新搜索关键字。 */
    fun updateSearch(value: String) {
        _state.update { it.copy(searchInfo = value) }
    }

    /**
     * 加载社团列表。
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
                repository.clubs(_state.value.searchInfo.ifBlank { null }, targetPage, 10)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        clubs = if (refresh) page.items else it.clubs + page.items,
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
                        loadState = LoadState(error = error.message ?: "社团列表加载失败")
                    )
                }
            }
        }
    }

    /** 如果后端还有下一页，就继续加载更多社团。 */
    fun loadNextPage() {
        if (_state.value.hasNext && !_state.value.loadingMore) {
            load(refresh = false)
        }
    }

    /** 提交加入社团申请。 */
    fun applyJoin(groupId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                repository.applyJoin(groupId)
            }.onSuccess { message ->
                _state.update { it.copy(loadState = LoadState(message = message)) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "申请提交失败"))
                }
            }
        }
    }
}
