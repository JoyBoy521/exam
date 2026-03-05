package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.AntiCheatRule;
import com.exam.system.entity.ExamCheatEvent;
import com.exam.system.entity.Student;
import com.exam.system.mapper.AntiCheatRuleMapper;
import com.exam.system.mapper.ExamCheatEventMapper;
import com.exam.system.mapper.StudentMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/cheat-events")
public class TeacherCheatController {

    private final ExamCheatEventMapper examCheatEventMapper;
    private final AntiCheatRuleMapper antiCheatRuleMapper;
    private final StudentMapper studentMapper;

    public TeacherCheatController(ExamCheatEventMapper examCheatEventMapper,
                                  AntiCheatRuleMapper antiCheatRuleMapper,
                                  StudentMapper studentMapper) {
        this.examCheatEventMapper = examCheatEventMapper;
        this.antiCheatRuleMapper = antiCheatRuleMapper;
        this.studentMapper = studentMapper;
    }

    @GetMapping("/risk-summary")
    public List<Map<String, Object>> riskSummary(@RequestParam Long examId) {
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
            rule = new AntiCheatRule();
            rule.setPageBlurWeight(5);
            rule.setWindowSwitchWeight(10);
            rule.setNetworkDisconnectWeight(8);
            rule.setCopyPasteWeight(20);
            rule.setOtherWeight(3);
            rule.setDurationStepSeconds(15);
            rule.setMediumRiskThreshold(40);
            rule.setHighRiskThreshold(70);
        }

        int durationStep = Math.max(rule.getDurationStepSeconds() == null ? 15 : rule.getDurationStepSeconds(), 1);

        Map<Long, List<ExamCheatEvent>> byStudent = events.stream().collect(Collectors.groupingBy(ExamCheatEvent::getStudentId));
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(byStudent.keySet()).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<ExamCheatEvent>> entry : byStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<ExamCheatEvent> studentEvents = entry.getValue();

            int totalRisk = 0;
            for (ExamCheatEvent event : studentEvents) {
                int weight = weightForType(rule, event.getType());
                int factor = Math.max(1, (event.getDurationSeconds() + durationStep - 1) / durationStep);
                totalRisk += weight * factor;
            }

            String level = totalRisk >= rule.getHighRiskThreshold() ? "HIGH" :
                    (totalRisk >= rule.getMediumRiskThreshold() ? "MEDIUM" : "LOW");

            Student student = studentMap.get(studentId);
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", studentId);
            row.put("studentNo", student != null ? student.getStudentNo() : String.valueOf(studentId));
            row.put("studentName", student != null ? student.getName() : ("学生" + studentId));
            row.put("eventCount", studentEvents.size());
            row.put("riskScore", totalRisk);
            row.put("riskLevel", level);
            result.add(row);
        }

        result.sort((a, b) -> Integer.compare((Integer) b.get("riskScore"), (Integer) a.get("riskScore")));
        return result;
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
}
