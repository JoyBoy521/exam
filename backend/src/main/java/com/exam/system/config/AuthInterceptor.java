package com.exam.system.config;

import com.exam.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String ATTR_USER_ID = "auth.userId";
    public static final String ATTR_ROLE = "auth.role";
    public static final String ATTR_LOGIN_NAME = "auth.loginName";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        AuthService.AuthPrincipal principal = authService.getPrincipal(token);
        if (principal == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未登录或登录已过期\"}");
            return false;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/teacher/") && !("TEACHER".equals(principal.role()) || "ADMIN".equals(principal.role()))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"无权限访问教师接口\"}");
            return false;
        }

        if (uri.startsWith("/api/student/") && !"STUDENT".equals(principal.role())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"无权限访问学生接口\"}");
            return false;
        }

        request.setAttribute(ATTR_USER_ID, principal.userId());
        request.setAttribute(ATTR_ROLE, principal.role());
        request.setAttribute(ATTR_LOGIN_NAME, principal.loginName());
        return true;
    }
}
