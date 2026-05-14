package com.chinahitech.shop.utils;

import com.chinahitech.shop.exception.BusinessException;
import org.springframework.security.core.Authentication;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "未认证或登录已过期");
        }
        return authentication.getName();
    }

    public static String requireSelf(Authentication authentication, String requestedUserId) {
        String currentUserId = currentUserId(authentication);
        if (requestedUserId == null || requestedUserId.trim().isEmpty()) {
            throw new BusinessException("PARAM_ERROR", "用户编号不能为空");
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new BusinessException("FORBIDDEN", "不能操作其他用户的数据");
        }
        return currentUserId;
    }
}
