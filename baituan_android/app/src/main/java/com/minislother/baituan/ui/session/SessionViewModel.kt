package com.minislother.baituan.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minislother.baituan.core.auth.AuthSession
import com.minislother.baituan.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * SessionViewModel 负责维护“当前是否登录、登录身份是什么”的状态。
 *
 * 根导航只观察这个 ViewModel，不直接读取 DataStore。这样导航层保持简单：未登录进入身份选择，
 * 已登录则根据 role 进入学生端、社团管理员端或超级管理员端。
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    val session = authRepository.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthSession(null, null))
}
