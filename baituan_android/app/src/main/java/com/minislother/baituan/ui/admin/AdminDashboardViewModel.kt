package com.minislother.baituan.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.data.AdminRepository
import com.minislother.baituan.data.AuthRepository
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.domain.model.ClubRelation
import com.minislother.baituan.domain.model.PlatformUser
import com.minislother.baituan.domain.model.RankingStat
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 管理端工作台状态。
 *
 * 同一个状态类同时服务社团管理员端和超级管理员端。不同身份只会填充自己用得到的字段，
 * 页面根据 role 选择展示哪些区块。
 */
data class AdminDashboardState(
    val loadState: LoadState = LoadState(),
    val managedClubs: List<Club> = emptyList(),
    val managedActivities: List<ClubActivity> = emptyList(),
    val pendingClubRelations: List<ClubRelation> = emptyList(),
    val users: List<PlatformUser> = emptyList(),
    val allClubs: List<Club> = emptyList(),
    val allActivities: List<ClubActivity> = emptyList(),
    val clubApplications: List<Club> = emptyList(),
    val activityApplications: List<ClubActivity> = emptyList(),
    val groupStats: List<RankingStat> = emptyList(),
    val activityStats: List<RankingStat> = emptyList()
)

/**
 * 管理员端和超级管理员端共用 ViewModel。
 *
 * ViewModel 根据身份加载不同数据：社团管理员只加载自己负责的内容；超级管理员加载平台级概览。
 */
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AdminDashboardState())
    val state = _state.asStateFlow()

    /** 根据身份刷新工作台数据。 */
    fun load(role: UserRole) {
        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true, error = null)) }
            runCatching {
                when (role) {
                    UserRole.Manager -> loadManagerDashboard()
                    UserRole.SuperAdmin -> loadSuperAdminDashboard()
                    UserRole.Student -> AdminDashboardState()
                }
            }.onSuccess { newState ->
                _state.value = newState.copy(loadState = LoadState())
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "数据加载失败，请稍后再试"))
                }
            }
        }
    }

    /** 社团管理员工作台：负责社团、负责活动、首个社团的待审核入团申请。 */
    private suspend fun loadManagerDashboard(): AdminDashboardState {
        val clubs = safeLoadList { adminRepository.managedClubs().items }
        val activities = safeLoadList { adminRepository.managedActivities().items }
        val pending = clubs.firstOrNull()?.let { club ->
            safeLoadList { adminRepository.clubJoinApplications(club.id).items }
        }.orEmpty()

        return AdminDashboardState(
            managedClubs = clubs,
            managedActivities = activities,
            pendingClubRelations = pending
        )
    }

    /** 超级管理员工作台：用户、社团、活动、审核和排行统计。 */
    private suspend fun loadSuperAdminDashboard(): AdminDashboardState {
        return AdminDashboardState(
            users = safeLoadList { adminRepository.allUsers().items },
            allClubs = safeLoadList { adminRepository.allClubs().items },
            allActivities = safeLoadList { adminRepository.allActivities().items },
            clubApplications = safeLoadList { adminRepository.clubApplications().items },
            activityApplications = safeLoadList { adminRepository.activityApplications().items },
            groupStats = safeLoadList { adminRepository.groupMemberStats() },
            activityStats = safeLoadList { adminRepository.activityMemberStats() }
        )
    }

    /**
     * 单个区块的数据加载兜底。
     *
     * 管理端首页会同时请求很多接口。如果某一个接口临时失败，返回空列表比让整个工作台失败更稳。
     * 外层仍然保留总的 runCatching，用来兜住真正不可恢复的异常。
     */
    private suspend fun <T> safeLoadList(block: suspend () -> List<T>): List<T> {
        return runCatching { block() }.getOrDefault(emptyList())
    }

    /** 退出当前身份。根导航会在本地登录态清空后自动回到身份选择页。 */
    fun logout(role: UserRole) {
        viewModelScope.launch {
            authRepository.logout(role)
        }
    }
}
