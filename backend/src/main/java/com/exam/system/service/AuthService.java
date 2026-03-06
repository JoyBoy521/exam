package com.exam.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.LoginResponse;
import com.exam.system.entity.Student;
import com.exam.system.entity.SysUser;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final long TOKEN_TTL_SECONDS = 12 * 60 * 60;

    public record AuthPrincipal(Long userId, String loginName, String displayName, String role) {}
    private record TokenSession(AuthPrincipal principal, Instant expireAt) {}

    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, TokenSession> tokenStore = new ConcurrentHashMap<>();

    public AuthService(StudentMapper studentMapper, SysUserMapper sysUserMapper) {
        this.studentMapper = studentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public LoginResponse login(String username, String password) {
        AuthPrincipal principal;

        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("limit 1")
        );
        if (sysUser != null) {
            if (!Integer.valueOf(1).equals(sysUser.getStatus())) {
                throw new IllegalArgumentException("账号已停用");
            }
            if (!matchesSysUserPassword(password, sysUser)) {
                throw new IllegalArgumentException("用户名或密码错误");
            }
            principal = new AuthPrincipal(sysUser.getId(), sysUser.getUsername(), sysUser.getDisplayName(), sysUser.getRole());
        } else {
            LambdaQueryWrapper<Student> query = new LambdaQueryWrapper<>();
            query.eq(Student::getStudentNo, username);
            Student student = studentMapper.selectOne(query);

            if (student == null || !matchesStudentPassword(password, student)) {
                throw new IllegalArgumentException("用户名或密码错误");
            }
            principal = new AuthPrincipal(student.getId(), student.getStudentNo(), student.getName(), "STUDENT");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, new TokenSession(principal, Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));

        return new LoginResponse(token, principal.displayName(), principal.role(), principal.userId());
    }

    public AuthPrincipal getPrincipal(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TokenSession session = tokenStore.get(token);
        if (session == null) {
            return null;
        }
        if (Instant.now().isAfter(session.expireAt())) {
            tokenStore.remove(token);
            return null;
        }
        return session.principal();
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenStore.remove(token);
        }
    }

    public void changeStudentPassword(Long studentId, String oldPassword, String newPassword) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        if (!matchesStudentPassword(oldPassword, student)) {
            throw new IllegalArgumentException("旧密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        studentMapper.updateById(student);

        // Force re-login on other active sessions of this student.
        tokenStore.entrySet().removeIf(e -> {
            AuthPrincipal p = e.getValue().principal();
            return "STUDENT".equals(p.role()) && studentId.equals(p.userId());
        });
    }

    public void changeTeacherPassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!matchesSysUserPassword(oldPassword, user)) {
            throw new IllegalArgumentException("旧密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        // Force re-login on other active sessions of this user.
        tokenStore.entrySet().removeIf(e -> {
            AuthPrincipal p = e.getValue().principal();
            return (("TEACHER".equals(p.role()) || "ADMIN".equals(p.role())) && userId.equals(p.userId()));
        });
    }

    private boolean matchesStudentPassword(String rawPassword, Student student) {
        String dbPassword = student.getPassword();
        if (dbPassword == null || dbPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcrypt(dbPassword)) {
            return passwordEncoder.matches(rawPassword, dbPassword);
        }

        boolean matched = dbPassword.equals(rawPassword);
        if (matched) {
            student.setPassword(passwordEncoder.encode(rawPassword));
            studentMapper.updateById(student);
        }
        return matched;
    }

    private boolean looksLikeBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private boolean matchesSysUserPassword(String rawPassword, SysUser user) {
        String dbPassword = user.getPassword();
        if (dbPassword == null || dbPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcrypt(dbPassword)) {
            return passwordEncoder.matches(rawPassword, dbPassword);
        }

        boolean matched = dbPassword.equals(rawPassword);
        if (matched) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
        }
        return matched;
    }
}
