package com.minislother.baituan.data

import com.google.gson.Gson
import com.minislother.baituan.core.auth.JwtSubjectParser
import com.minislother.baituan.core.auth.TokenStore
import com.minislother.baituan.core.network.BaituanApi
import com.minislother.baituan.core.network.readList
import com.minislother.baituan.core.network.readObject
import com.minislother.baituan.core.network.readPage
import com.minislother.baituan.core.network.requireSuccess
import com.minislother.baituan.domain.model.ActivityRelation
import com.minislother.baituan.domain.model.ApplicationRecord
import com.minislother.baituan.domain.model.Club
import com.minislother.baituan.domain.model.Student
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProfileRepository 负责“我的”页面需要的数据。
 *
 * 这里会从 token 中解析当前学生学号，然后调用后端获取个人资料、我的社团、
 * 我的活动和我的入团申请。这样页面不需要知道 JWT 的结构。
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val api: BaituanApi,
    private val tokenStore: TokenStore,
    private val gson: Gson
) {
    /** 从 JWT token 的 subject 字段中解析当前学生学号。 */
    suspend fun currentStudentNumber(): String? {
        return JwtSubjectParser.subject(tokenStore.token.first())
    }

    /** 查询当前学生的个人资料。 */
    suspend fun profile(): Student? {
        val stuNumber = currentStudentNumber() ?: return null
        return api.studentProfile(stuNumber).requireSuccess().data.readObject(gson, "student")
    }

    /** 分页查询当前学生提交过的入团申请。 */
    suspend fun myApplications(pageNum: Int, pageSize: Int): PagedData<ApplicationRecord> {
        val stuNumber = currentStudentNumber().orEmpty()
        val data = api.myApplications(stuNumber, pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }

    /** 查询当前学生已加入或已申请的社团。 */
    suspend fun myClubs(): List<Club> {
        return api.myClubs().requireSuccess().data.readList(gson, "groupItems")
    }

    /** 分页查询当前学生已报名或已申请的活动关系。 */
    suspend fun myActivities(pageNum: Int, pageSize: Int): PagedData<ActivityRelation> {
        val data = api.myActivities(pageNum, pageSize).requireSuccess().data
        return PagedData(
            items = data.readList(gson, "items"),
            page = data.readPage(gson)
        )
    }
}
