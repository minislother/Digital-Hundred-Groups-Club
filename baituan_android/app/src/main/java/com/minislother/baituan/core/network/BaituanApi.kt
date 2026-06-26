package com.minislother.baituan.core.network

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 学生注册请求体。
 *
 * 后端 RegisterUser 接收的字段就是 userId/password/email/valicode。
 * Android 端把注册表单整理成这个对象后，Retrofit 会自动把它序列化成 JSON。
 */
data class RegisterRequest(
    val userId: String,
    val password: String,
    val email: String,
    val valicode: String
)

/**
 * 后端接口定义。
 *
 * Retrofit 会根据这里的注解生成真实网络请求。Repository 层只调用这些函数，不需要自己拼 URL、
 * 创建 HTTP 请求或解析请求方法。为了方便理解，这个文件按“认证、学生端、管理员端、超级管理员端”
 * 分组排列。
 */
interface BaituanApi {
    // region 认证接口

    /** 学生登录，成功后后端会在 data.token 中返回 JWT。 */
    @FormUrlEncoded
    @POST("student/login")
    suspend fun studentLogin(
        @Field("stuNumber") stuNumber: String,
        @Field("password") password: String
    ): ApiEnvelope

    /** 社团管理员登录，账号字段名由后端定义为 userId。 */
    @FormUrlEncoded
    @POST("manager/login")
    suspend fun managerLogin(
        @Field("userId") userId: String,
        @Field("password") password: String
    ): ApiEnvelope

    /** 超级管理员登录，账号字段名同样是 userId。 */
    @FormUrlEncoded
    @POST("topManager/login")
    suspend fun topManagerLogin(
        @Field("userId") userId: String,
        @Field("password") password: String
    ): ApiEnvelope

    /** 学生注册，注册前需要先获取邮箱验证码。 */
    @POST("student/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope

    /** 发送邮箱验证码，用于学生注册时验证邮箱。 */
    @POST("student/validateEmail")
    suspend fun validateEmail(@Query("email") email: String): ApiEnvelope

    /** 学生退出登录。Android 收到响应后会清理本地 token。 */
    @POST("student/logout")
    suspend fun studentLogout(): ApiEnvelope

    /** 社团管理员退出登录。 */
    @POST("manager/logout")
    suspend fun managerLogout(): ApiEnvelope

    /** 超级管理员退出登录。 */
    @POST("topManager/logout")
    suspend fun topManagerLogout(): ApiEnvelope

    // endregion

    // region 学生端接口

    /** 根据 token 查询当前学生的基础信息。 */
    @GET("student/info")
    suspend fun studentInfo(): ApiEnvelope

    /** 查询学生完整个人资料。后端要求传入学号。 */
    @FormUrlEncoded
    @POST("student/profile")
    suspend fun studentProfile(@Field("stuNumber") stuNumber: String): ApiEnvelope

    /** 查询热门社团，用于学生端首页推荐区。 */
    @GET("group/top")
    suspend fun topClubs(): ApiEnvelope

    /** 分页查询社团列表，可按 searchInfo 搜索。 */
    @GET("group/all")
    suspend fun clubs(
        @Query("searchInfo") searchInfo: String?,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询指定社团详情。 */
    @FormUrlEncoded
    @POST("group/studentDetail")
    suspend fun clubDetail(@Field("groupName") groupName: String): ApiEnvelope

    /** 当前登录学生申请加入某个社团。 */
    @FormUrlEncoded
    @POST("individualGroup/applyJoinGroup")
    suspend fun applyJoinClub(@Field("groupId") groupId: Int): ApiEnvelope

    /** 查询当前学生已加入或已申请的社团。 */
    @POST("individualGroup/allGroups")
    suspend fun myClubs(): ApiEnvelope

    /** 查询当前学生已报名或已申请的活动。 */
    @POST("individualActivity/allActivities")
    suspend fun myActivities(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询热门活动，用于学生端首页推荐区。 */
    @GET("activity/top")
    suspend fun topActivities(): ApiEnvelope

    /** 分页查询活动列表，可按 searchInfo 搜索。 */
    @GET("activity/all")
    suspend fun activities(
        @Query("searchInfo") searchInfo: String?,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询活动详情。后端使用社团名和活动名定位活动。 */
    @FormUrlEncoded
    @POST("activity/studentDetail")
    suspend fun activityDetail(
        @Field("groupName") groupName: String,
        @Field("activityName") activityName: String
    ): ApiEnvelope

    /** 当前登录学生报名某个活动。 */
    @FormUrlEncoded
    @POST("individualActivity/joinActivity")
    suspend fun joinActivity(@Field("activityId") activityId: Int): ApiEnvelope

    /** 查询当前学生提交过的入团申请记录。 */
    @FormUrlEncoded
    @POST("application/myApps")
    suspend fun myApplications(
        @Field("stuNumber") stuNumber: String,
        @Field("pageNum") pageNum: Int,
        @Field("pageSize") pageSize: Int
    ): ApiEnvelope

    // endregion

    // region 社团管理员端接口

    /** 查询当前管理员负责的社团列表。 */
    @GET("individualGroup/allManagedGroups")
    suspend fun managedClubs(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询当前管理员负责的活动列表。 */
    @GET("individualActivity/allManagedActivities")
    suspend fun managedActivities(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询指定社团的待审核入团申请。 */
    @GET("individualGroup/getGroupApplyList")
    suspend fun clubJoinApplications(
        @Query("groupId") groupId: Int,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 批准学生加入社团。 */
    @FormUrlEncoded
    @POST("individualGroup/acceptJoin")
    suspend fun acceptClubJoin(
        @Field("groupId") groupId: Int,
        @Field("studentId") studentId: String
    ): ApiEnvelope

    /** 拒绝学生加入社团。 */
    @FormUrlEncoded
    @POST("individualGroup/rejectJoin")
    suspend fun rejectClubJoin(
        @Field("groupId") groupId: Int,
        @Field("studentId") studentId: String
    ): ApiEnvelope

    // endregion

    // region 超级管理员端接口

    /** 查询全平台用户，超级管理员端用于用户管理。 */
    @GET("topManager/getAllUsers")
    suspend fun allUsers(
        @Query("searchInfo") searchInfo: String?,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询所有待审核或历史审核社团申请。 */
    @GET("group/allApps")
    suspend fun clubApplications(
        @Query("searchInfo") searchInfo: String?,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询所有待审核或历史审核活动申请。 */
    @GET("activity/allApps")
    suspend fun activityApplications(
        @Query("searchInfo") searchInfo: String?,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope

    /** 查询社团成员数量排行，超级管理员数据看板使用。 */
    @GET("individualGroup/getGroupMembers")
    suspend fun groupMemberStats(): ApiEnvelope

    /** 查询活动成员数量排行，超级管理员数据看板使用。 */
    @GET("individualActivity/getActivityMembers")
    suspend fun activityMemberStats(): ApiEnvelope

    // endregion
}
