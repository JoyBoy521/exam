package com.exam.system.util;

import com.exam.system.config.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;

public final class CurrentUser {
    private CurrentUser() {}

    public static Long userId(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (value instanceof Long id) {
            return id;
        }
        throw new IllegalStateException("未识别到登录用户");
    }

    public static String role(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_ROLE);
        return value == null ? "" : value.toString();
    }

    public static String loginName(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_LOGIN_NAME);
        return value == null ? "" : value.toString();
    }
}
