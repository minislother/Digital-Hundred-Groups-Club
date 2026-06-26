package com.minislother.baituan.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.data.AuthRepository
import com.minislother.baituan.data.ProfileRepository
import com.minislother.baituan.domain.model.ActivityRelation
import com.minislother.baituan.domain.model.ApplicationRecord
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.Student
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * “我的”页面状态。
 *
 * 这个页面同时展示个人资料、我的社团、我的活动和我的入团申请，
 * 所以状态对象里会同时保存多组数据。
 */
data class ProfileUiState(
    val student: Student? = null,
    val clubs: List<Club> = emptyList(),
    val activities: List<ActivityRelation> = emptyList(),
    val applications: List<ApplicationRecord> = emptyList(),
    val applicationPageNum: Int = 1,
    val hasMoreApplications: Boolean = false,
    val loadingMoreApplications: Boolean = false,
    val loadState: LoadState = LoadState()
)

/**
 * “我的”页面 ViewModel。
 *
 * 它会并发加载多个接口，减少用户等待时间。页面刷新时加载个人资料、社团、
 * 活动和第一页申请；点击“加载更多申请”时只继续加载申请列表。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    /**
     * 加载个人中心数据。
     *
     * refresh=true 表示刷新整个个人中心；refresh=false 只加载下一页申请记录。
     */
    fun load(refresh: Boolean = true) {
        viewModelScope.launch {
            val targetPage = if (refresh) 1 else _state.value.applicationPageNum + 1
            _state.update {
                if (refresh) {
                    it.copy(loadState = LoadState(loading = true))
                } else {
                    it.copy(loadingMoreApplications = true, loadState = LoadState())
                }
            }
            runCatching {
                val profile = if (refresh) async { profileRepository.profile() } else null
                val clubs = if (refresh) async { profileRepository.myClubs() } else null
                val activities = if (refresh) async { profileRepository.myActivities(1, 5) } else null
                val applications = async { profileRepository.myApplications(targetPage, 10) }
                val applicationPage = applications.await()
                ProfileLoadResult(
                    student = profile?.await(),
                    clubs = clubs?.await(),
                    activities = activities?.await()?.items,
                    applications = applicationPage.items,
                    hasMoreApplications = applicationPage.page?.hasNext == true
                )
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        student = result.student ?: it.student,
                        clubs = result.clubs ?: it.clubs,
                        activities = result.activities ?: it.activities,
                        applications = if (refresh) result.applications else it.applications + result.applications,
                        applicationPageNum = targetPage,
                        hasMoreApplications = result.hasMoreApplications,
                        loadingMoreApplications = false,
                        loadState = LoadState()
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loadingMoreApplications = false,
                        loadState = LoadState(error = error.message ?: "个人信息加载失败")
                    )
                }
            }
        }
    }

    /** 加载更多入团申请记录。 */
    fun loadMoreApplications() {
        if (_state.value.hasMoreApplications && !_state.value.loadingMoreApplications) {
            load(refresh = false)
        }
    }

    /** 退出登录并清除本地 token。 */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout(UserRole.Student)
        }
    }
}

/**
 * ProfileViewModel 内部使用的临时结果对象。
 *
 * 因为个人中心一次会加载多种数据，用这个对象可以让 onSuccess 中的赋值更清楚。
 */
private data class ProfileLoadResult(
    val student: Student?,
    val clubs: List<Club>?,
    val activities: List<ActivityRelation>?,
    val applications: List<ApplicationRecord>,
    val hasMoreApplications: Boolean
)
