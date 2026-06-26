package com.minislother.baituan.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 创建一个名为 auth 的 DataStore。
 *
 * DataStore 是 Android 官方推荐的小型本地存储方案。这里用它保存登录 token 和用户身份，
 * 这样 App 关闭后再次打开，也能知道用户之前登录的是学生端、管理员端还是超级管理员端。
 */
private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * 当前登录会话。
 *
 * token 是后端返回的 JWT；role 表示这个 token 属于哪个端。两者必须同时存在，才算有效登录态。
 */
data class AuthSession(
    val token: String?,
    val role: UserRole?
) {
    val isLoggedIn: Boolean = !token.isNullOrBlank() && role != null
}

/**
 * TokenStore 专门负责保存和读取登录态。
 *
 * Repository 和网络层都不直接碰 DataStore 的字段名，而是通过这个类读写。这样以后如果要把
 * 存储方式换成加密存储，只需要改 TokenStore，业务页面不用跟着改。
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("jwt_token")
    private val roleKey = stringPreferencesKey("user_role")

    /** 网络拦截器只需要 token，所以保留一个单独的 token Flow。 */
    val token: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[tokenKey]
    }

    /** 根导航需要同时知道 token 和身份，用来决定进入哪个端。 */
    val session: Flow<AuthSession> = combine(
        token,
        context.authDataStore.data.map { preferences -> UserRole.fromStorageValue(preferences[roleKey]) }
    ) { token, role ->
        AuthSession(token = token, role = role)
    }

    /** 登录成功后保存 JWT 和本次选择的身份。 */
    suspend fun save(token: String, role: UserRole) {
        context.authDataStore.edit { preferences ->
            preferences[tokenKey] = token
            preferences[roleKey] = role.storageValue
        }
    }

    /** 退出登录时同时清除 token 和身份，避免下一次打开 App 误进旧端。 */
    suspend fun clear() {
        context.authDataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(roleKey)
        }
    }
}
