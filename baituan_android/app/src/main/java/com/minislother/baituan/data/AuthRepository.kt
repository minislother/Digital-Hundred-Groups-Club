package com.minislother.baituan.data

import com.minislother.baituan.core.auth.AuthSession
import com.minislother.baituan.core.auth.TokenStore
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.core.network.ApiException
import com.minislother.baituan.core.network.BaituanApi
import com.minislother.baituan.core.network.RegisterRequest
import com.minislother.baituan.core.network.readString
import com.minislother.baituan.core.network.requireSuccess
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository 负责所有“账号、登录态、退出登录”相关的数据操作。
 *
 * 页面只关心“用哪个身份登录、是否登录成功”。具体调用 `/student/login` 还是
 * `/manager/login`，以及 token 怎样保存，都集中放在这个 Repository 里。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: BaituanApi,
    private val tokenStore: TokenStore
) {
    /** 根导航通过 session 判断当前是否已登录，以及已登录的是哪个端。 */
    val session: Flow<AuthSession> = tokenStore.session

    /**
     * 根据身份调用对应登录接口。
     *
     * 学生端后端字段名叫 stuNumber；管理员和超级管理员字段名叫 userId。Repository 把这个差异
     * 包起来，LoginViewModel 不需要知道这些接口细节。
     */
    suspend fun login(role: UserRole, account: String, password: String) {
        val response = when (role) {
            UserRole.Student -> api.studentLogin(account, password)
            UserRole.Manager -> api.managerLogin(account, password)
            UserRole.SuperAdmin -> api.topManagerLogin(account, password)
        }.requireSuccess()

        val token = response.data.readString("token")
            ?: throw ApiException("登录成功但后端没有返回 token")
        tokenStore.save(token, role)
    }

    /** 请求后端给邮箱发送验证码，目前注册流程只开放给学生端。 */
    suspend fun sendValidateEmail(email: String): String {
        return api.validateEmail(email).requireSuccess().message
    }

    /** 注册学生账号。validateCode 对应后端字段 valicode。 */
    suspend fun register(
        stuNumber: String,
        password: String,
        email: String,
        validateCode: String
    ): String {
        return api.register(
            RegisterRequest(
                userId = stuNumber,
                password = password,
                email = email,
                valicode = validateCode
            )
        ).requireSuccess().message
    }

    /** 退出登录：按身份通知对应后端接口，然后清除本地 token 与身份。 */
    suspend fun logout(role: UserRole?) {
        runCatching {
            when (role) {
                UserRole.Manager -> api.managerLogout()
                UserRole.SuperAdmin -> api.topManagerLogout()
                else -> api.studentLogout()
            }
        }
        tokenStore.clear()
    }
}
