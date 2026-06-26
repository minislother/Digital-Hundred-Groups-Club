package com.minislother.baituan.core.auth

/**
 * App 支持的三种登录身份。
 *
 * 每个身份对应后端的一组接口前缀：学生使用 `/student`，社团管理员使用 `/manager`，
 * 超级管理员使用 `/topManager`。把身份集中定义在这里，可以避免页面里到处写散落的字符串。
 */
enum class UserRole(
    val storageValue: String,
    val displayName: String,
    val accountLabel: String,
    val loginDescription: String
) {
    Student(
        storageValue = "student",
        displayName = "学生端",
        accountLabel = "学号",
        loginDescription = "浏览社团、报名活动、查看自己的申请进度。"
    ),
    Manager(
        storageValue = "manager",
        displayName = "社团管理员端",
        accountLabel = "管理员账号",
        loginDescription = "维护社团资料、审核入团申请、管理成员和活动。"
    ),
    SuperAdmin(
        storageValue = "super_admin",
        displayName = "超级管理员端",
        accountLabel = "超级管理员账号",
        loginDescription = "管理全平台用户、社团、活动和批量导入任务。"
    );

    companion object {
        /** 根据本地存储的字符串还原身份；如果历史数据不存在或不认识，就返回 null。 */
        fun fromStorageValue(value: String?): UserRole? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}
