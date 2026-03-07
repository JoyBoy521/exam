package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.AntiCheatRule;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamCheatEvent;
import com.exam.system.entity.ExamHeartbeat;
import com.exam.system.entity.Student;
import com.exam.system.mapper.AntiCheatRuleMapper;
import com.exam.system.mapper.ExamCheatEventMapper;
import com.exam.system.mapper.ExamHeartbeatMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.StudentMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/monitor")
public class TeacherMonitorController {

    private final ExamMapper examMapper;
    private final StudentMapper studentMapper;
    private final ExamHeartbeatMapper examHeartbeatMapper;
    private final ExamCheatEventMapper examCheatEventMapper;
    private final AntiCheatRuleMapper antiCheatRuleMapper;

    public TeacherMonitorController(ExamMapper examMapper,
                                    StudentMapper studentMapper,
                                    ExamHeartbeatMapper examHeartbeatMapper,
                                    ExamCheatEventMapper examCheatEventMapper,
                                    AntiCheatRuleMapper antiCheatRuleMapper) {
        this.examMapper = examMapper;
        this.studentMapper = studentMapper;
        this.examHeartbeatMapper = examHeartbeatMapper;
        this.examCheatEventMapper = examCheatEventMapper;
        this.antiCheatRuleMapper = antiCheatRuleMapper;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam Long examId,
                                         @RequestParam(required = false) String riskLevel,
                                         @RequestParam(required = false) Long classId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        Long effectiveClassId = classId == null ? exam.getClassId() : classId;

        int activeWindowSeconds = 90;
        LocalDateTime onlineAfter = LocalDateTime.now().minusSeconds(activeWindowSeconds);

        long totalStudents = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, effectiveClassId)
        );
        Map<Long, Student> classStudentMap = studentMapper.selectList(
                        new LambdaQueryWrapper<Student>().eq(Student::getClassId, effectiveClassId)
                ).stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));

        List<ExamHeartbeat> onlineHeartbeats = examHeartbeatMapper.selectList(
                new LambdaQueryWrapper<ExamHeartbeat>()
                        .eq(ExamHeartbeat::getExamId, examId)
                        .ge(ExamHeartbeat::getUpdatedAt, onlineAfter)
                        .orderByDesc(ExamHeartbeat::getUpdatedAt)
        );
        Map<Long, ExamHeartbeat> latestByStudent = new HashMap<>();
        for (ExamHeartbeat hb : onlineHeartbeats) {
            if (!classStudentMap.containsKey(hb.getStudentId())) {
                continue;
            }
            latestByStudent.putIfAbsent(hb.getStudentId(), hb);
        }

        List<Map<String, Object>> onlineStudents = latestByStudent.values().stream()
                .sorted(Comparator.comparing(ExamHeartbeat::getUpdatedAt).reversed())
                .map(hb -> {
                    Student student = classStudentMap.get(hb.getStudentId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("studentId", hb.getStudentId());
                    row.put("studentNo", student != null ? student.getStudentNo() : String.valueOf(hb.getStudentId()));
                    row.put("studentName", student != null ? student.getName() : ("学生" + hb.getStudentId()));
                    row.put("answeredCount", hb.getAnsweredCount());
                    row.put("totalCount", hb.getTotalCount());
                    row.put("timeLeftSeconds", hb.getTimeLeftSeconds());
                    row.put("updatedAt", hb.getUpdatedAt());
                    return row;
                })
                .toList();

        List<ExamCheatEvent> recentEventEntities = examCheatEventMapper.selectList(
                new LambdaQueryWrapper<ExamCheatEvent>()
                        .eq(ExamCheatEvent::getExamId, examId)
                        .orderByDesc(ExamCheatEvent::getHappenedAt)
                        .last("limit 30")
        );
        List<Map<String, Object>> riskTopAll = buildRiskTop(examId, effectiveClassId);
        Map<Long, String> riskLevelByStudent = riskTopAll.stream().collect(Collectors.toMap(
                row -> ((Number) row.get("studentId")).longValue(),
                row -> String.valueOf(row.get("riskLevel")),
                (a, b) -> a
        ));
        List<Map<String, Object>> riskTop = riskTopAll.stream()
                .filter(row -> normalizedRiskLevel == null || normalizedRiskLevel.equals(String.valueOf(row.get("riskLevel"))))
                .limit(10)
                .toList();

        List<Map<String, Object>> recentEvents = new ArrayList<>();
        for (ExamCheatEvent event : recentEventEntities) {
            Student student = classStudentMap.get(event.getStudentId());
            if (student == null) {
                continue;
            }
            String level = riskLevelByStudent.getOrDefault(event.getStudentId(), "LOW");
            if (normalizedRiskLevel != null && !normalizedRiskLevel.equals(level)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", event.getId());
            row.put("studentId", event.getStudentId());
            row.put("studentNo", student.getStudentNo());
            row.put("studentName", student.getName());
            row.put("type", event.getType());
            row.put("durationSeconds", event.getDurationSeconds());
            row.put("detail", event.getDetail());
            row.put("happenedAt", event.getHappenedAt());
            row.put("riskLevel", level);
            recentEvents.add(row);
        }
        List<Map<String, Object>> onlineTrend = buildOnlineTrend(examId, effectiveClassId);

        Map<String, Object> result = new HashMap<>();
        result.put("examId", exam.getId());
        result.put("examTitle", exam.getTitle());
        result.put("classId", effectiveClassId);
        result.put("riskLevel", normalizedRiskLevel);
        result.put("activeWindowSeconds", activeWindowSeconds);
        result.put("onlineCount", onlineStudents.size());
        result.put("totalStudents", totalStudents);
        result.put("onlineRate", totalStudents == 0 ? 0 : Math.round(onlineStudents.size() * 10000.0 / totalStudents) / 100.0);
        result.put("onlineTrend", onlineTrend);
        result.put("onlineStudents", onlineStudents);
        result.put("recentViolations", recentEvents);
        result.put("riskTopStudents", riskTop);
        return result;
    }

    private List<Map<String, Object>> buildRiskTop(Long examId, Long classId) {
        List<ExamCheatEvent> events = examCheatEventMapper.selectList(
                new LambdaQueryWrapper<ExamCheatEvent>()
                        .eq(ExamCheatEvent::getExamId, examId)
                        .orderByDesc(ExamCheatEvent::getHappenedAt)
        );
        if (events.isEmpty()) {
            return List.of();
        }

        AntiCheatRule rule = antiCheatRuleMapper.selectOne(
                new LambdaQueryWrapper<AntiCheatRule>().orderByDesc(AntiCheatRule::getUpdatedAt).last("limit 1")
        );
        if (rule == null) {
            rule = defaultRule();
        }
        int durationStep = Math.max(rule.getDurationStepSeconds() == null ? 15 : rule.getDurationStepSeconds(), 1);

        Map<Long, List<ExamCheatEvent>> byStudent = events.stream().collect(Collectors.groupingBy(ExamCheatEvent::getStudentId));
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(byStudent.keySet()).stream()
                .filter(s -> classId == null || classId.equals(s.getClassId()))
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<ExamCheatEvent>> entry : byStudent.entrySet()) {
            Long studentId = entry.getKey();
            Student student = studentMap.get(studentId);
            if (student == null) {
                continue;
            }
            List<ExamCheatEvent> studentEvents = entry.getValue();

            int totalRisk = 0;
            for (ExamCheatEvent event : studentEvents) {
                int weight = weightForType(rule, event.getType());
                int factor = Math.max(1, (event.getDurationSeconds() + durationStep - 1) / durationStep);
                totalRisk += weight * factor;
            }

            String level = totalRisk >= rule.getHighRiskThreshold() ? "HIGH" :
                    (totalRisk >= rule.getMediumRiskThreshold() ? "MEDIUM" : "LOW");

            Map<String, Object> row = new HashMap<>();
            row.put("studentId", studentId);
            row.put("studentNo", student.getStudentNo());
            row.put("studentName", student.getName());
            row.put("eventCount", studentEvents.size());
            row.put("riskScore", totalRisk);
            row.put("riskLevel", level);
            result.add(row);
        }

        result.sort((a, b) -> Integer.compare((Integer) b.get("riskScore"), (Integer) a.get("riskScore")));
        return result;
    }

    private List<Map<String, Object>> buildOnlineTrend(Long examId, Long classId) {
        long totalStudents = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId)
        );
        if (totalStudents <= 0) {
            return List.of(
                    trendRow(5, 0, 0),
                    trendRow(15, 0, 0),
                    trendRow(30, 0, 0)
            );
        }
        Set<Long> classStudentIds = studentMapper.selectList(
                        new LambdaQueryWrapper<Student>()
                                .select(Student::getId)
                                .eq(Student::getClassId, classId)
                ).stream()
                .map(Student::getId)
                .collect(Collectors.toSet());
        if (classStudentIds.isEmpty()) {
            return List.of(
                    trendRow(5, 0, 0),
                    trendRow(15, 0, 0),
                    trendRow(30, 0, 0)
            );
        }
        int[] windows = {5, 15, 30};
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int minutes : windows) {
            LocalDateTime after = now.minusMinutes(minutes);
            List<ExamHeartbeat> beats = examHeartbeatMapper.selectList(
                    new LambdaQueryWrapper<ExamHeartbeat>()
                            .eq(ExamHeartbeat::getExamId, examId)
                            .ge(ExamHeartbeat::getUpdatedAt, after)
            );
            long count = beats.stream()
                    .map(ExamHeartbeat::getStudentId)
                    .filter(classStudentIds::contains)
                    .distinct()
                    .count();
            double rate = Math.round(count * 10000.0 / totalStudents) / 100.0;
            trend.add(trendRow(minutes, count, rate));
        }
        return trend;
    }

    private Map<String, Object> trendRow(int minutes, long count, double rate) {
        Map<String, Object> row = new HashMap<>();
        row.put("windowMinutes", minutes);
        row.put("onlineCount", count);
        row.put("onlineRate", rate);
        return row;
    }

    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        String normalized = riskLevel.trim().toUpperCase();
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private int weightForType(AntiCheatRule rule, String type) {
        if ("PAGE_BLUR".equals(type)) {
            return rule.getPageBlurWeight();
        }
        if ("WINDOW_SWITCH".equals(type)) {
            return rule.getWindowSwitchWeight();
        }
        if ("NETWORK_DISCONNECT".equals(type)) {
            return rule.getNetworkDisconnectWeight();
        }
        if ("COPY_PASTE".equals(type)) {
            return rule.getCopyPasteWeight();
        }
        return rule.getOtherWeight();
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
}
