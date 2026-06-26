package com.minislother.baituan.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 注册页面 UI。
 *
 * 用户在这里输入学号、邮箱、验证码和密码。页面本身只负责展示表单；
 * 发送验证码、校验输入和注册请求都交给 RegisterViewModel 处理。
 */
@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 注册成功后返回登录页，让用户使用新账号登录。
    LaunchedEffect(state.registered) {
        if (state.registered) {
            onBackToLogin()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 注册页说明区，帮助用户理解需要哪些信息。
            Text(text = "注册学生账号", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "使用学号、邮箱验证码和密码创建账号。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))

            // 学号输入框，对应后端注册字段 userId。
            OutlinedTextField(
                value = state.stuNumber,
                onValueChange = viewModel::updateStuNumber,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("学号") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "学号") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 邮箱输入框，用于接收验证码。
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("邮箱") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = "邮箱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 验证码输入框和发送按钮放在同一行，符合移动端常见注册体验。
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.validateCode,
                    onValueChange = viewModel::updateValidateCode,
                    modifier = Modifier.weight(1f),
                    label = { Text("验证码") },
                    leadingIcon = { Icon(Icons.Outlined.Verified, contentDescription = "验证码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedButton(
                    onClick = viewModel::sendValidateEmail,
                    enabled = !state.emailSending,
                    modifier = Modifier.height(64.dp)
                ) {
                    Text(if (state.emailSending) "发送中" else "发送")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 密码输入框，提供显示/隐藏密码按钮。
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = "密码") },
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisible) {
                        Icon(
                            imageVector = if (state.passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (state.passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                visualTransformation = if (state.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            // 展示注册错误或验证码发送成功等提示。
            if (state.loadState.error != null || state.loadState.message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.loadState.error ?: state.loadState.message.orEmpty(),
                    color = if (state.loadState.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 注册请求进行中时禁用按钮，防止重复提交。
            Button(
                onClick = viewModel::register,
                enabled = !state.loadState.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (state.loadState.loading) "注册中" else "注册")
            }

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("已有账号？返回登录")
            }
        }
    }
}
