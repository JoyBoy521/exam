package com.exam.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.LoginResponse;
import com.exam.system.dto.RegisterStudentRequest;
import com.exam.system.entity.ClassInfo;
import com.exam.system.entity.Student;
import com.exam.system.entity.SysUser;
import com.exam.system.mapper.ClassInfoMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_TTL_SECONDS = 12 * 60 * 60;
    private static final String TOKEN_KEY_PREFIX = "exam:auth:token:";
    private static final String USER_TOKENS_KEY_PREFIX = "exam:auth:user:";
    private static final String USER_TOKENS_KEY_SUFFIX = ":tokens";

    public record AuthPrincipal(Long userId, String loginName, String displayName, String role) {}
    private record TokenSession(AuthPrincipal principal, Instant expireAt) {}

    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassInfoMapper classInfoMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, TokenSession> tokenStore = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public AuthService(StudentMapper studentMapper,
                       SysUserMapper sysUserMapper,
                       ClassInfoMapper classInfoMapper,
                       ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.studentMapper = studentMapper;
        this.sysUserMapper = sysUserMapper;
        this.classInfoMapper = classInfoMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public LoginResponse login(String username, String password) {
        AuthPrincipal principal;

        SysUser sysUser = findSysUserCompat(username);
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
        saveSession(token, principal, Instant.now().plusSeconds(TOKEN_TTL_SECONDS));

        return new LoginResponse(token, principal.displayName(), principal.role(), principal.userId());
    }

    public AuthPrincipal getPrincipal(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        AuthPrincipal redisPrincipal = getPrincipalFromRedis(token);
        if (redisPrincipal != null) {
            return redisPrincipal;
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
            deleteSessionInRedis(token);
        }
    }

    public void registerStudent(RegisterStudentRequest request) {
        long duplicatedStudentNo = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, request.studentNo().trim())
        );
        if (duplicatedStudentNo > 0) {
            throw new IllegalArgumentException("学号已存在");
        }
        ClassInfo classInfo = classInfoMapper.selectById(request.classId());
        if (classInfo == null) {
            throw new IllegalArgumentException("班级不存在");
        }

        Student student = new Student();
        student.setStudentNo(request.studentNo().trim());
        student.setName(request.name().trim());
        student.setClassId(request.classId());
        student.setPassword(passwordEncoder.encode(request.password()));
        student.setCreateTime(LocalDateTime.now());
        studentMapper.insert(student);

        classInfo.setStudentCount((classInfo.getStudentCount() == null ? 0 : classInfo.getStudentCount()) + 1);
        classInfoMapper.updateById(classInfo);
    }

    public List<ClassInfo> listRegisterClasses() {
        try {
            return classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>().orderByAsc(ClassInfo::getId));
        } catch (Exception ex) {
            log.error("listRegisterClasses failed, return empty list to keep login page available", ex);
            return List.of();
        }
    }

    private SysUser findSysUserCompat(String username) {
        try {
            return sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("limit 1")
            );
        } catch (Exception ex) {
            log.warn("sys_user modern query failed, fallback to legacy real_name mapping: {}", ex.getMessage());
            try {
                return sysUserMapper.selectLegacyByUsername(username);
            } catch (Exception fallbackEx) {
                log.error("sys_user legacy fallback query failed", fallbackEx);
                return null;
            }
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
        invalidateUserRedisSessions("STUDENT", studentId);
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
        invalidateUserRedisSessions("TEACHER", userId);
        invalidateUserRedisSessions("ADMIN", userId);
    }

    private void saveSession(String token, AuthPrincipal principal, Instant expireAt) {
        tokenStore.put(token, new TokenSession(principal, expireAt));
        saveSessionToRedis(token, principal, expireAt);
    }

    private void saveSessionToRedis(String token, AuthPrincipal principal, Instant expireAt) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String value = principal.userId() + "|"
                    + encodeSegment(principal.loginName()) + "|"
                    + encodeSegment(principal.displayName()) + "|"
                    + principal.role();
            Duration ttl = Duration.ofSeconds(Math.max(1, expireAt.getEpochSecond() - Instant.now().getEpochSecond()));
            String tokenKey = tokenKey(token);
            redisTemplate.opsForValue().set(tokenKey, value, ttl);
            String userTokensKey = userTokensKey(principal.role(), principal.userId());
            redisTemplate.opsForSet().add(userTokensKey, token);
            redisTemplate.expire(userTokensKey, ttl);
        } catch (Exception ignored) {
            // Redis 不可用时回退内存会话
        }
    }

    private AuthPrincipal getPrincipalFromRedis(String token) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(tokenKey(token));
            if (value == null || value.isBlank()) {
                return null;
            }
            String[] arr = value.split("\\|", -1);
            if (arr.length < 4) {
                return null;
            }
            Long userId = Long.valueOf(arr[0]);
            String loginName = decodeSegment(arr[1]);
            String displayName = decodeSegment(arr[2]);
            String role = arr[3];
            return new AuthPrincipal(userId, loginName, displayName, role);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void deleteSessionInRedis(String token) {
        if (redisTemplate == null) {
            return;
        }
        try {
            AuthPrincipal principal = getPrincipalFromRedis(token);
            redisTemplate.delete(tokenKey(token));
            if (principal != null) {
                redisTemplate.opsForSet().remove(userTokensKey(principal.role(), principal.userId()), token);
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void invalidateUserRedisSessions(String role, Long userId) {
        if (redisTemplate == null || userId == null) {
            return;
        }
        try {
            String key = userTokensKey(role, userId);
            var tokens = redisTemplate.opsForSet().members(key);
            if (tokens != null && !tokens.isEmpty()) {
                redisTemplate.delete(tokens.stream().map(this::tokenKey).toList());
            }
            redisTemplate.delete(key);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String userTokensKey(String role, Long userId) {
        return USER_TOKENS_KEY_PREFIX + role + ":" + userId + USER_TOKENS_KEY_SUFFIX;
    }

    private String encodeSegment(String value) {
        String safe = value == null ? "" : value;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
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
