package com.minislother.baituan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.data.ActivityRepository
import com.minislother.baituan.data.ClubRepository
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val clubs: List<Club> = emptyList(),
    val activities: List<ClubActivity> = emptyList(),
    val loadState: LoadState = LoadState()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    /**
     * 加载页面所需数据，并同步到当前状态。
     */
    fun load() {
        if (_state.value.loadState.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                val clubs = async { clubRepository.topClubs() }
                val activities = async { activityRepository.topActivities() }
                clubs.await() to activities.await()
            }.onSuccess { (clubs, activities) ->
                _state.update {
                    it.copy(
                        clubs = clubs,
                        activities = activities,
                        loadState = LoadState()
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "首页加载失败"))
                }
            }
        }
    }
}
