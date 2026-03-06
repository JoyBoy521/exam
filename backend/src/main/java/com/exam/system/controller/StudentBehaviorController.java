package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.CheatEventReportRequest;
import com.exam.system.entity.AntiCheatRule;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamCheatEvent;
import com.exam.system.entity.Student;
import com.exam.system.mapper.AntiCheatRuleMapper;
import com.exam.system.mapper.ExamCheatEventMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.service.TeacherRiskPushService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/student/exams")
public class StudentBehaviorController {

    private final ExamCheatEventMapper examCheatEventMapper;
    private final ExamMapper examMapper;
    private final StudentMapper studentMapper;
    private final AntiCheatRuleMapper antiCheatRuleMapper;
    private final TeacherRiskPushService teacherRiskPushService;
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "PAGE_BLUR", "WINDOW_SWITCH", "NETWORK_DISCONNECT", "COPY_PASTE", "FULLSCREEN_EXIT", "OTHER"
    ));

    public StudentBehaviorController(ExamCheatEventMapper examCheatEventMapper,
                                     ExamMapper examMapper,
                                     StudentMapper studentMapper,
                                     AntiCheatRuleMapper antiCheatRuleMapper,
                                     TeacherRiskPushService teacherRiskPushService) {
        this.examCheatEventMapper = examCheatEventMapper;
        this.examMapper = examMapper;
        this.studentMapper = studentMapper;
        this.antiCheatRuleMapper = antiCheatRuleMapper;
        this.teacherRiskPushService = teacherRiskPushService;
    }

    @GetMapping("/{examId}/anti-cheat-rule")
    public Map<String, Object> getAntiCheatRule(@PathVariable Long examId, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        validateStudentExam(examId, studentId);

        AntiCheatRule rule = antiCheatRuleMapper.selectOne(
                new LambdaQueryWrapper<AntiCheatRule>().orderByDesc(AntiCheatRule::getUpdatedAt).last("limit 1")
        );
        if (rule == null) {
            rule = defaultRule();
        }

        int pageBlurWeight = safeNumber(rule.getPageBlurWeight(), 5);
        int mediumThreshold = safeNumber(rule.getMediumRiskThreshold(), 40);
        int highThreshold = safeNumber(rule.getHighRiskThreshold(), 70);
        int maxPageBlurCount = Math.max(1, (mediumThreshold + pageBlurWeight - 1) / pageBlurWeight);

        Map<String, Object> result = new HashMap<>();
        result.put("pageBlurWeight", pageBlurWeight);
        result.put("windowSwitchWeight", safeNumber(rule.getWindowSwitchWeight(), 10));
        result.put("networkDisconnectWeight", safeNumber(rule.getNetworkDisconnectWeight(), 8));
        result.put("copyPasteWeight", safeNumber(rule.getCopyPasteWeight(), 20));
        result.put("otherWeight", safeNumber(rule.getOtherWeight(), 3));
        result.put("durationStepSeconds", safeNumber(rule.getDurationStepSeconds(), 15));
        result.put("mediumRiskThreshold", mediumThreshold);
        result.put("highRiskThreshold", highThreshold);
        result.put("maxPageBlurCount", maxPageBlurCount);
        return result;
    }

    @PostMapping("/{examId}/cheat-events")
    public String reportCheatEvent(@PathVariable Long examId,
                                   @Valid @RequestBody CheatEventReportRequest payload,
                                   HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        validateStudentExam(examId, studentId);
        String type = normalizeEventType(payload.type());
        int durationSeconds = normalizeDuration(payload.durationSeconds());
        LocalDateTime now = LocalDateTime.now();

        ExamCheatEvent latestSameType = examCheatEventMapper.selectOne(
                new LambdaQueryWrapper<ExamCheatEvent>()
                        .eq(ExamCheatEvent::getExamId, examId)
                        .eq(ExamCheatEvent::getStudentId, studentId)
                        .eq(ExamCheatEvent::getType, type)
                        .orderByDesc(ExamCheatEvent::getHappenedAt)
                        .last("limit 1")
        );
        if (latestSameType != null && latestSameType.getHappenedAt() != null) {
            long secondsBetween = ChronoUnit.SECONDS.between(latestSameType.getHappenedAt(), now);
            if (secondsBetween >= 0 && secondsBetween < 2) {
                return "已忽略重复上报";
            }
        }

        ExamCheatEvent event = new ExamCheatEvent();
        event.setExamId(examId);
        event.setStudentId(studentId);
        event.setType(type);
        event.setDurationSeconds(durationSeconds);
        event.setDetail(payload.detail());
        event.setHappenedAt(now);
        examCheatEventMapper.insert(event);
        teacherRiskPushService.pushCheatEvent(examId, studentId, type, durationSeconds);
        return "已上报";
    }

    private void validateStudentExam(Long examId, Long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        if (exam.getClassId() != null && student.getClassId() != null && !exam.getClassId().equals(student.getClassId())) {
            throw new IllegalArgumentException("无权限访问该考试");
        }
    }

    private AntiCheatRule defaultRule() {
        AntiCheatRule rule = new AntiCheatRule();
        rule.setPageBlurWeight(5);
        rule.setWindowSwitchWeight(10);
        rule.setNetworkDisconnectWeight(8);
        rule.setCopyPasteWeight(20);
        rule.setOtherWeight(3);
        rule.setDurationStepSeconds(15);
        rule.setMediumRiskThreshold(40);
        rule.setHighRiskThreshold(70);
        return rule;
    }

    private int safeNumber(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int normalizeDuration(Integer rawDuration) {
        if (rawDuration == null) {
            return 1;
        }
        return Math.max(1, Math.min(rawDuration, 3600));
    }

    private String normalizeEventType(String rawType) {
        String normalized = rawType == null ? "OTHER" : rawType.trim().toUpperCase();
        if (normalized.isBlank()) {
            return "OTHER";
        }
        return ALLOWED_TYPES.contains(normalized) ? normalized : "OTHER";
    }
}
