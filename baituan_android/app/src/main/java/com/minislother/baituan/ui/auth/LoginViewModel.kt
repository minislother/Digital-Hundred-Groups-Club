package com.minislother.baituan.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.core.auth.UserRole
import com.minislother.baituan.data.AuthRepository
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页的完整页面状态。
 *
 * account 保存当前输入的账号。对学生来说它是学号，对管理员和超级管理员来说它是用户编号。
 * 页面展示什么标签由 UserRole 决定，状态本身只关心“账号值”。
 */
data class LoginFormState(
    val account: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val loadState: LoadState = LoadState()
)

/**
 * 登录页 ViewModel。
 *
 * ViewModel 处理输入、密码可见性和登录请求。它不画 UI，只把处理后的状态暴露给 LoginScreen。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginFormState())
    val state = _state.asStateFlow()

    /** 更新账号输入框内容，并清除旧错误提示。 */
    fun updateAccount(value: String) {
        _state.update { it.copy(account = value, loadState = it.loadState.copy(error = null)) }
    }

    /** 更新密码输入框内容，并清除旧错误提示。 */
    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, loadState = it.loadState.copy(error = null)) }
    }

    /** 切换密码明文/隐藏显示。 */
    fun togglePasswordVisible() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * 校验输入并调用对应身份的登录接口。
     *
     * 登录成功后 AuthRepository 会保存 token 和身份；根导航监听到登录态变化后会自动进入对应主界面。
     */
    fun login(role: UserRole) {
        val current = _state.value
        if (current.account.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(loadState = LoadState(error = "请输入${role.accountLabel}和密码")) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                authRepository.login(role, current.account.trim(), current.password)
            }.onSuccess {
                _state.update { it.copy(loadState = LoadState()) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "登录失败，请稍后再试"))
                }
            }
        }
    }
}
