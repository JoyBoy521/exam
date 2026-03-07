package com.exam.system.controller;

import com.exam.system.dto.ChangePasswordRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.SysUser;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.SysUserMapper;
import com.exam.system.service.AuthService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/profile")
public class TeacherProfileController {

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;

    public TeacherProfileController(AuthService authService,
                                    SysUserMapper sysUserMapper,
                                    ExamMapper examMapper,
                                    ExamRecordMapper examRecordMapper) {
        this.authService = authService;
        this.sysUserMapper = sysUserMapper;
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                 HttpServletRequest httpRequest) {
        Long userId = CurrentUser.userId(httpRequest);
        authService.changeTeacherPassword(userId, request.oldPassword(), request.newPassword());
        return "密码修改成功";
    }

    @GetMapping("/info")
    public Map<String, Object> info(HttpServletRequest request) {
        Long userId = CurrentUser.userId(request);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        long examCount = examMapper.selectCount(null);
        long markingCount = examRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getStatus, "MARKING")
        );
        long ongoingExamCount = examMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Exam>()
                        .eq(Exam::getStatus, "ONGOING")
        );

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("displayName", user.getDisplayName());
        m.put("role", user.getRole());
        m.put("status", user.getStatus());
        m.put("createTime", user.getCreateTime());
        m.put("examCount", examCount);
        m.put("ongoingExamCount", ongoingExamCount);
        m.put("markingCount", markingCount);
        return m;
    }

    @PostMapping("/update-basic")
    public String updateBasic(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long userId = CurrentUser.userId(request);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        String displayName = payload.get("displayName") == null ? null : String.valueOf(payload.get("displayName")).trim();
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("显示名称不能为空");
        }
        user.setDisplayName(displayName);
        user.setUpdateTime(java.time.LocalDateTime.now());
        sysUserMapper.updateById(user);
        return "资料更新成功";
    }
}
