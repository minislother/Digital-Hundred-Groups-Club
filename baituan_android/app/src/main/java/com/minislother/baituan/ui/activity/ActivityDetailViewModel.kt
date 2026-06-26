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

data class ActivityDetailUiState(
    val activity: ClubActivity? = null,
    val loadState: LoadState = LoadState()
)

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ActivityDetailUiState())
    val state = _state.asStateFlow()

    /**
     * 加载页面所需数据，并同步到当前状态。
     */
    fun load(groupName: String, activityName: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                repository.detail(groupName, activityName)
            }.onSuccess { activity ->
                _state.update { it.copy(activity = activity, loadState = LoadState()) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "活动详情加载失败"))
                }
            }
        }
    }

    /**
     * 处理当前模块的业务状态或界面事件。
     */
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
