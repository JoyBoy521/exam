package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.SysUser;
import com.exam.system.mapper.SysUserMapper;
import com.exam.system.service.AuditLogService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/accounts")
public class TeacherAccountController {

    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TeacherAccountController(SysUserMapper sysUserMapper, AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String role,
                                    @RequestParam(required = false) Integer status,
                                    HttpServletRequest request) {
        ensureAdmin(request);
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
        List<Map<String, Object>> rows = users.stream()
                .map(this::toSafeRow)
                .filter(x -> hitFilter(x, keyword, role, status))
                .toList();
        return paginate(rows, page, size);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        ensureAdmin(request);
        String username = String.valueOf(payload.getOrDefault("username", "")).trim();
        String displayName = String.valueOf(payload.getOrDefault("displayName", "")).trim();
        String role = String.valueOf(payload.getOrDefault("role", "TEACHER")).trim().toUpperCase();
        if (username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("角色仅支持 TEACHER 或 ADMIN");
        }
        long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        auditLogService.record(
                CurrentUser.userId(request),
                "CREATE_SYS_USER",
                "SYS_USER",
                user.getId(),
                "username=" + username + ",role=" + role
        );
        return toSafeRow(user);
    }

    @PutMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        ensureAdmin(request);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        int status = Integer.parseInt(String.valueOf(payload.getOrDefault("status", "1")));
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("状态仅支持 0/1");
        }
        if (CurrentUser.userId(request).equals(id) && status == 0) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        auditLogService.record(
                CurrentUser.userId(request),
                "UPDATE_SYS_USER_STATUS",
                "SYS_USER",
                user.getId(),
                "status=" + status
        );
        return "状态已更新";
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        auditLogService.record(
                CurrentUser.userId(request),
                "RESET_SYS_USER_PASSWORD",
                "SYS_USER",
                user.getId(),
                "username=" + user.getUsername()
        );
        Map<String, String> result = new HashMap<>();
        result.put("message", "密码已重置为 123456");
        return result;
    }

    private void ensureAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(CurrentUser.role(request))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
    }

    private Map<String, Object> toSafeRow(SysUser user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("displayName", user.getDisplayName());
        row.put("role", user.getRole());
        row.put("status", user.getStatus());
        row.put("createTime", user.getCreateTime());
        row.put("updateTime", user.getUpdateTime());
        return row;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> rows, Integer page, Integer size) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.max(1, Math.min(size == null ? 10 : size, 200));
        int total = rows.size();
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows.subList(fromIndex, toIndex));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private boolean hitFilter(Map<String, Object> row, String keyword, String role, Integer status) {
        if (role != null && !role.isBlank()) {
            if (!role.trim().equalsIgnoreCase(String.valueOf(row.getOrDefault("role", "")))) {
                return false;
            }
        }
        if (status != null) {
            int rowStatus = Integer.parseInt(String.valueOf(row.getOrDefault("status", "0")));
            if (rowStatus != status) {
                return false;
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            return String.valueOf(row.getOrDefault("username", "")).toLowerCase().contains(kw)
                    || String.valueOf(row.getOrDefault("displayName", "")).toLowerCase().contains(kw);
        }
        return true;
    }
}
