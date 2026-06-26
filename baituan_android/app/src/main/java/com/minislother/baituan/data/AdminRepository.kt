package com.minislother.baituan.data

import com.google.gson.Gson
import com.minislother.baituan.core.network.BaituanApi
import com.minislother.baituan.core.network.readList
import com.minislother.baituan.core.network.readPage
import com.minislother.baituan.core.network.requireSuccess
import com.minislother.baituan.domain.model.ActivityRelation
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.ClubActivity
import com.minislother.baituan.domain.model.ClubRelation
import com.minislother.baituan.domain.model.PlatformUser
import com.minislother.baituan.domain.model.RankingStat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理员端和超级管理员端共用的数据仓库。
 *
 * 这个类只负责“拿数据”和“把 JSON 转成 Kotlin 模型”。页面不直接依赖 Retrofit，
 * 这样以后要增加缓存、分页加载或统一错误处理时，只需要优先改 Repository。
 */
@Singleton
class AdminRepository @Inject constructor(
    private val api: BaituanApi,
    private val gson: Gson
) {
    /** 查询当前社团管理员负责的社团。 */
    suspend fun managedClubs(pageNum: Int = 1, pageSize: Int = 20): PagedData<Club> {
        val data = api.managedClubs(pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 查询当前社团管理员负责的活动。 */
    suspend fun managedActivities(pageNum: Int = 1, pageSize: Int = 20): PagedData<ClubActivity> {
        val data = api.managedActivities(pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 查询某个社团的待审核入团申请。 */
    suspend fun clubJoinApplications(
        groupId: Int,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PagedData<ClubRelation> {
        val data = api.clubJoinApplications(groupId, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 批准或拒绝入团申请后，返回后端提示信息。 */
    suspend fun reviewClubJoin(groupId: Int, studentId: String, accepted: Boolean): String {
        val response = if (accepted) {
            api.acceptClubJoin(groupId, studentId)
        } else {
            api.rejectClubJoin(groupId, studentId)
        }.requireSuccess()
        return response.message
    }

    /** 超级管理员查询平台全部用户。 */
    suspend fun allUsers(searchInfo: String? = null, pageNum: Int = 1, pageSize: Int = 20): PagedData<PlatformUser> {
        val data = api.allUsers(searchInfo, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 超级管理员查询全部社团。 */
    suspend fun allClubs(searchInfo: String? = null, pageNum: Int = 1, pageSize: Int = 20): PagedData<Club> {
        val data = api.clubs(searchInfo, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 超级管理员查询全部活动。 */
    suspend fun allActivities(
        searchInfo: String? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PagedData<ClubActivity> {
        val data = api.activities(searchInfo, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 超级管理员查询待审核或历史审核社团申请。 */
    suspend fun clubApplications(pageNum: Int = 1, pageSize: Int = 20): PagedData<Club> {
        val data = api.clubApplications(null, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 超级管理员查询待审核或历史审核活动申请。 */
    suspend fun activityApplications(pageNum: Int = 1, pageSize: Int = 20): PagedData<ClubActivity> {
        val data = api.activityApplications(null, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 查询社团人数排行，用于超级管理员看板。 */
    suspend fun groupMemberStats(): List<RankingStat> {
        return api.groupMemberStats().requireSuccess().data.readList(gson, "items")
    }

    /** 查询活动人数排行，用于超级管理员看板。 */
    suspend fun activityMemberStats(): List<RankingStat> {
        return api.activityMemberStats().requireSuccess().data.readList(gson, "items")
    }
}
