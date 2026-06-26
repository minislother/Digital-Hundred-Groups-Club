package com.minislother.baituan.data

import com.google.gson.Gson
import com.minislother.baituan.core.network.BaituanApi
import com.minislother.baituan.core.network.PageInfo
import com.minislother.baituan.core.network.readList
import com.minislother.baituan.core.network.readObject
import com.minislother.baituan.core.network.readPage
import com.minislother.baituan.core.network.requireSuccess
import com.minislother.baituan.domain.model.Club
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通用分页数据容器。
 *
 * items 是当前页数据，page 是后端返回的分页信息。社团、活动、申请列表都可以复用。
 */
data class PagedData<T>(
    val items: List<T>,
    val page: PageInfo? = null
)

/**
 * ClubRepository 负责社团相关数据。
 *
 * ViewModel 不直接调用 Retrofit，而是调用 Repository。这样后续如果要增加缓存、
 * 本地数据库或统一错误处理，只需要改 Repository，不需要改页面代码。
 */
@Singleton
class ClubRepository @Inject constructor(
    private val api: BaituanApi,
    private val gson: Gson
) {
    /** 首页使用的热门社团列表。 */
    suspend fun topClubs(): List<Club> {
        return api.topClubs().requireSuccess().data.readList(gson, "item")
    }

    /** 社团列表页使用的分页搜索接口。 */
    suspend fun clubs(searchInfo: String?, pageNum: Int, pageSize: Int): PagedData<Club> {
        val data = api.clubs(searchInfo, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 根据社团名称查询社团详情。 */
    suspend fun detail(groupName: String): Club? {
        return api.clubDetail(groupName).requireSuccess().data.readObject(gson, "group")
    }

    /** 当前登录学生申请加入指定社团，返回后端提示信息。 */
    suspend fun applyJoin(groupId: Int): String {
        return api.applyJoinClub(groupId).requireSuccess().message
    }
}
