package com.minislother.baituan.data

import com.google.gson.Gson
import com.minislother.baituan.core.network.BaituanApi
import com.minislother.baituan.core.network.readList
import com.minislother.baituan.core.network.readObject
import com.minislother.baituan.core.network.readPage
import com.minislother.baituan.core.network.requireSuccess
import com.minislother.baituan.domain.model.ClubActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ActivityRepository 负责活动相关数据。
 *
 * 它把后端的 JSON 响应转换成 App 内部使用的 ClubActivity 模型，
 * 页面只需要拿到活动列表、活动详情或报名结果。
 */
@Singleton
class ActivityRepository @Inject constructor(
    private val api: BaituanApi,
    private val gson: Gson
) {
    /** 首页使用的热门活动列表。 */
    suspend fun topActivities(): List<ClubActivity> {
        return api.topActivities().requireSuccess().data.readList(gson, "item")
    }

    /** 活动列表页使用的分页搜索接口。 */
    suspend fun activities(searchInfo: String?, pageNum: Int, pageSize: Int): PagedData<ClubActivity> {
        val data = api.activities(searchInfo, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 查询某个活动的详情。后端要求同时传社团名和活动名。 */
    suspend fun detail(groupName: String, activityName: String): ClubActivity? {
        return api.activityDetail(groupName, activityName).requireSuccess().data.readObject(gson, "activity")
    }

    /** 当前登录学生报名活动，返回后端提示信息。 */
    suspend fun join(activityId: Int): String {
        return api.joinActivity(activityId).requireSuccess().message
    }
}
