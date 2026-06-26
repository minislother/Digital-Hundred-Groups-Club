package com.minislother.baituan.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 学生资料模型，对应后端 User。
 *
 * `userId` 在后端 JSON 中就是学号。学生端资料页、申请记录和成员列表都会复用这个模型。
 */
data class Student(
    @SerializedName("userId") val userId: String = "",
    val userName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val campus: String? = null,
    val school: String? = null,
    val major: String? = null,
    val description: String? = null,
    val status: Int = 0,
    val nickname: String? = null,
    val avatar: String? = null
)

/**
 * 平台用户模型，同样对应后端 User，但用于超级管理员端用户管理。
 *
 * 这里故意不包含 password 和 salt，因为移动端页面不应该读取、展示或缓存任何登录凭据字段。
 */
data class PlatformUser(
    @SerializedName("userId") val userId: String = "",
    val userName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val campus: String? = null,
    val school: String? = null,
    val major: String? = null,
    val description: String? = null,
    val status: Int = 0,
    val nickname: String? = null,
    val avatar: String? = null,
    val createTime: String? = null,
    val modifyTime: String? = null
) {
    /** 后端用 status 表示身份，App 在这里转成用户能看懂的文字。 */
    val roleName: String
        get() = when {
            status >= 10 -> "超级管理员"
            status >= 1 -> "社团管理员"
            else -> "学生"
        }
}

/**
 * 社团模型，对应后端 Group。
 *
 * 列表页、详情页、管理员“我管理的社团”和超级管理员社团审批都会使用这个模型。
 */
data class Club(
    val id: Int = 0,
    val name: String = "",
    val leader: String? = null,
    val image: String? = null,
    val description: String? = null,
    val attachment: String? = null,
    val hot: Int = 0,
    val isAccepted: Boolean? = null
)

/**
 * 活动模型，对应后端 Activity。
 *
 * 因为 Activity 是 Android 系统里的常见类名，所以这里命名为 ClubActivity，避免和 Android
 * Activity 概念混淆。
 */
data class ClubActivity(
    val id: Int = 0,
    val name: String = "",
    val organizer: String? = null,
    val image: String? = null,
    val description: String? = null,
    val attachment: String? = null,
    val hot: Int = 0,
    val arrange: String? = null,
    val time: String? = null,
    val number: Int = 0,
    val place: String? = null,
    val type: Int = 0,
    val groupName: String? = null,
    val isAccepted: Boolean? = null
)

/**
 * 入团申请记录，对应后端 StuApp。
 *
 * 学生端用于“我的入团申请”，管理员端用于“待审核入团申请”。
 */
data class ApplicationRecord(
    val applicationId: Int = 0,
    val groupName: String = "",
    val stuName: String? = null,
    val stuNumber: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val major: String? = null,
    val selfIntro: String? = null,
    val attachment: String? = null,
    val isAccepted: Boolean? = null,
    val createTime: String? = null
)

/**
 * 学生和社团之间的关系记录，对应后端 IndividualGroup。
 *
 * 管理员端的成员列表与入团申请列表都会使用它。status 表示申请或成员状态，具体含义由后端业务决定。
 */
data class ClubRelation(
    val id: Int = 0,
    val groupId: Int = 0,
    val studentId: String? = null,
    val position: String? = null,
    val userName: String? = null,
    val status: Int = 0,
    val createTime: String? = null
)

/**
 * 学生和活动之间的关系记录，对应后端 IndividualActivity。
 *
 * 学生端“我的活动”和管理员端活动报名审核都会使用这个模型。
 */
data class ActivityRelation(
    val id: Int = 0,
    val activityId: Int = 0,
    val studentId: String? = null,
    val position: String? = null,
    val userName: String? = null,
    val status: Int = 0,
    val isAccepted: Boolean? = null,
    val createTime: String? = null
)

/**
 * 排名统计项。
 *
 * 后端的社团人数排行字段叫 groupName/count，活动人数排行字段可能叫 activityName/count。
 * 为了让一个 UI 组件能同时展示两类排行，这里提供 displayName 统一取展示名称。
 */
data class RankingStat(
    val id: Int = 0,
    val activityId: Int = 0,
    val groupId: Int = 0,
    val groupName: String? = null,
    val activityName: String? = null,
    val name: String? = null,
    val count: Int = 0,
    val memberNum: Int = 0,
    val hot: Int = 0
) {
    val displayName: String
        get() = groupName ?: activityName ?: name ?: "未命名"

    val displayCount: Int
        get() = when {
            count > 0 -> count
            memberNum > 0 -> memberNum
            else -> 0
        }
}
