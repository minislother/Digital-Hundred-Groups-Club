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

data class ClubDetailUiState(
    val club: Club? = null,
    val loadState: LoadState = LoadState()
)

@HiltViewModel
class ClubDetailViewModel @Inject constructor(
    private val repository: ClubRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ClubDetailUiState())
    val state = _state.asStateFlow()

    /**
     * 加载页面所需数据，并同步到当前状态。
     */
    fun load(groupName: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                repository.detail(groupName)
            }.onSuccess { club ->
                _state.update { it.copy(club = club, loadState = LoadState()) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "社团详情加载失败"))
                }
            }
        }
    }

    /**
     * 处理当前模块的业务状态或界面事件。
     */
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
