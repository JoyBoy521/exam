package com.exam.system.controller;

import com.exam.system.dto.LoginRequest;
import com.exam.system.dto.LoginResponse;
import com.exam.system.dto.RegisterStudentRequest;
import com.exam.system.entity.ClassInfo;
import com.exam.system.mapper.ClassInfoMapper;
import com.exam.system.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final ClassInfoMapper classInfoMapper;

    public AuthController(AuthService authService, ClassInfoMapper classInfoMapper) {
        this.authService = authService;
        this.classInfoMapper = classInfoMapper;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/classes")
    public List<Map<String, Object>> listRegisterClasses() {
        try {
            List<ClassInfo> list = classInfoMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassInfo>()
                            .orderByAsc(ClassInfo::getId)
            );
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ClassInfo c : list) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getId());
                row.put("name", c.getName());
                row.put("studentCount", c.getStudentCount());
                rows.add(row);
            }
            return rows;
        } catch (Throwable ex) {
            log.error("listRegisterClasses failed, fallback to empty list", ex);
            return List.of();
        }
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterStudentRequest request) {
        authService.registerStudent(request);
        return "注册成功";
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        authService.logout(token);
        return "已退出登录";
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
