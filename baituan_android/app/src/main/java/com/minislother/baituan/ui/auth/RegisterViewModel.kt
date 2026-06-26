package com.minislother.baituan.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.data.AuthRepository
import com.minislother.baituan.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 注册页的完整页面状态。
 *
 * 它保存注册表单的输入内容、验证码发送状态、是否注册成功以及错误/成功提示。
 */
data class RegisterFormState(
    val stuNumber: String = "",
    val password: String = "",
    val email: String = "",
    val validateCode: String = "",
    val passwordVisible: Boolean = false,
    val emailSending: Boolean = false,
    val registered: Boolean = false,
    val loadState: LoadState = LoadState()
)

/**
 * 注册页 ViewModel。
 *
 * 它负责表单校验、发送邮箱验证码、调用注册接口。页面只负责展示状态和把用户事件
 * 转发给这里。
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterFormState())
    val state = _state.asStateFlow()

    /** 更新学号输入。 */
    fun updateStuNumber(value: String) {
        _state.update { it.copy(stuNumber = value, loadState = it.loadState.copy(error = null)) }
    }

    /** 更新密码输入。 */
    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, loadState = it.loadState.copy(error = null)) }
    }

    /** 更新邮箱输入。 */
    fun updateEmail(value: String) {
        _state.update { it.copy(email = value, loadState = it.loadState.copy(error = null)) }
    }

    /** 更新用户收到的邮箱验证码。 */
    fun updateValidateCode(value: String) {
        _state.update { it.copy(validateCode = value, loadState = it.loadState.copy(error = null)) }
    }

    /** 切换密码明文/隐藏显示。 */
    fun togglePasswordVisible() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /** 请求后端发送邮箱验证码。 */
    fun sendValidateEmail() {
        val email = _state.value.email.trim()
        if (!email.contains("@")) {
            _state.update { it.copy(loadState = LoadState(error = "请先填写有效邮箱")) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(emailSending = true, loadState = LoadState()) }
            runCatching {
                authRepository.sendValidateEmail(email)
            }.onSuccess { message ->
                _state.update {
                    it.copy(emailSending = false, loadState = LoadState(message = message.ifBlank { "验证码已发送" }))
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        emailSending = false,
                        loadState = LoadState(error = error.message ?: "验证码发送失败")
                    )
                }
            }
        }
    }

    /** 校验注册表单并提交注册请求。 */
    fun register() {
        val current = _state.value
        val error = when {
            current.stuNumber.trim().length < 6 -> "请输入正确的学号"
            current.password.length < 6 -> "密码长度不能少于 6 位"
            !current.email.contains("@") -> "请输入有效邮箱"
            current.validateCode.isBlank() -> "请输入邮箱验证码"
            else -> null
        }

        if (error != null) {
            _state.update { it.copy(loadState = LoadState(error = error)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loadState = LoadState(loading = true)) }
            runCatching {
                authRepository.register(
                    stuNumber = current.stuNumber.trim(),
                    password = current.password,
                    email = current.email.trim(),
                    validateCode = current.validateCode.trim()
                )
            }.onSuccess { message ->
                _state.update {
                    it.copy(
                        registered = true,
                        loadState = LoadState(message = message.ifBlank { "注册成功，请登录" })
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(loadState = LoadState(error = error.message ?: "注册失败，请稍后再试"))
                }
            }
        }
    }
}
