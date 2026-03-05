package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.*;
import com.exam.system.mapper.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/statistics")
public class TeacherStatisticsController {

    private final ExamRecordMapper examRecordMapper;
    private final ExamCheatEventMapper examCheatEventMapper;
    private final StudentMapper studentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final AntiCheatRuleMapper antiCheatRuleMapper;

    public TeacherStatisticsController(ExamRecordMapper examRecordMapper,
                                       ExamCheatEventMapper examCheatEventMapper,
                                       StudentMapper studentMapper,
                                       ClassInfoMapper classInfoMapper,
                                       AntiCheatRuleMapper antiCheatRuleMapper) {
        this.examRecordMapper = examRecordMapper;
        this.examCheatEventMapper = examCheatEventMapper;
        this.studentMapper = studentMapper;
        this.classInfoMapper = classInfoMapper;
        this.antiCheatRuleMapper = antiCheatRuleMapper;
    }

    @GetMapping("/advanced")
    public Map<String, Object> advanced() {
        Map<String, Object> result = new HashMap<>();
        result.put("submissionTrend", buildSubmissionTrend());
        result.put("abnormalClassRanking", buildAbnormalClassRanking());
        result.put("passRateLayers", buildPassRateLayers(result));
        return result;
    }

    private List<Map<String, Object>> buildSubmissionTrend() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        Map<LocalDate, Long> dateCount = new HashMap<>();
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>().isNotNull(ExamRecord::getSubmitTime)
        );
        for (ExamRecord record : records) {
            LocalDateTime submitTime = record.getSubmitTime();
            if (submitTime == null) {
                continue;
            }
            LocalDate day = submitTime.toLocalDate();
            if (day.isBefore(from) || day.isAfter(today)) {
                continue;
            }
            dateCount.merge(day, 1L, Long::sum);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = from.plusDays(i);
            Map<String, Object> row = new HashMap<>();
            row.put("date", day.toString());
            row.put("count", dateCount.getOrDefault(day, 0L));
            trend.add(row);
        }
        return trend;
    }

    private List<Map<String, Object>> buildAbnormalClassRanking() {
        List<ExamCheatEvent> events = examCheatEventMapper.selectList(null);
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

        Set<Long> studentIds = events.stream().map(ExamCheatEvent::getStudentId).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, ClassInfo> classMap = classInfoMapper.selectList(null).stream()
                .collect(Collectors.toMap(ClassInfo::getId, c -> c));

        Map<Long, ClassRiskAgg> byClass = new HashMap<>();
        for (ExamCheatEvent event : events) {
            Student student = studentMap.get(event.getStudentId());
            if (student == null || student.getClassId() == null) {
                continue;
            }
            Long classId = student.getClassId();
            ClassRiskAgg agg = byClass.computeIfAbsent(classId, x -> new ClassRiskAgg());
            agg.eventCount++;
            agg.riskStudentIds.add(student.getId());

            int weight = weightForType(rule, event.getType());
            int duration = event.getDurationSeconds() == null ? 0 : event.getDurationSeconds();
            int factor = Math.max(1, (duration + durationStep - 1) / durationStep);
            agg.totalRisk += (weight * factor);
        }

        return byClass.entrySet().stream()
                .map(entry -> {
                    Long classId = entry.getKey();
                    ClassRiskAgg agg = entry.getValue();
                    ClassInfo classInfo = classMap.get(classId);
                    Map<String, Object> row = new HashMap<>();
                    row.put("classId", classId);
                    row.put("className", classInfo != null ? classInfo.getName() : ("班级" + classId));
                    row.put("eventCount", agg.eventCount);
                    row.put("riskStudentCount", agg.riskStudentIds.size());
                    row.put("riskScore", agg.totalRisk);
                    row.put("avgRiskScore", agg.riskStudentIds.isEmpty() ? 0 : Math.round((agg.totalRisk * 1.0 / agg.riskStudentIds.size()) * 100.0) / 100.0);
                    return row;
                })
                .sorted((a, b) -> {
                    int c = Integer.compare(((Number) b.get("riskScore")).intValue(), ((Number) a.get("riskScore")).intValue());
                    if (c != 0) {
                        return c;
                    }
                    return Integer.compare(((Number) b.get("eventCount")).intValue(), ((Number) a.get("eventCount")).intValue());
                })
                .limit(10)
                .toList();
    }

    private List<Map<String, Object>> buildPassRateLayers(Map<String, Object> resultContainer) {
        List<ExamRecord> records = examRecordMapper.selectList(null);
        long graded = 0;
        long excellent = 0;
        long good = 0;
        long pass = 0;
        long fail = 0;
        long pending = 0;

        for (ExamRecord record : records) {
            if (!"GRADED".equals(record.getStatus())) {
                pending++;
                continue;
            }
            BigDecimal total = record.getTotalScore();
            double score = total == null ? 0.0 : total.doubleValue();
            graded++;
            if (score >= 90) {
                excellent++;
            } else if (score >= 80) {
                good++;
            } else if (score >= 60) {
                pass++;
            } else {
                fail++;
            }
        }

        List<Map<String, Object>> layers = new ArrayList<>();
        layers.add(layerRow("优秀(>=90)", excellent, graded));
        layers.add(layerRow("良好(80-89)", good, graded));
        layers.add(layerRow("及格(60-79)", pass, graded));
        layers.add(layerRow("不及格(<60)", fail, graded));

        double overallPassRate = graded == 0 ? 0 : Math.round(((excellent + good + pass) * 10000.0 / graded)) / 100.0;
        resultContainer.put("gradedCount", graded);
        resultContainer.put("pendingCount", pending);
        resultContainer.put("overallPassRate", overallPassRate);
        return layers;
    }

    private Map<String, Object> layerRow(String label, long count, long total) {
        Map<String, Object> row = new HashMap<>();
        row.put("label", label);
        row.put("count", count);
        double rate = total == 0 ? 0 : Math.round(count * 10000.0 / total) / 100.0;
        row.put("rate", rate);
        return row;
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

    private static class ClassRiskAgg {
        int eventCount = 0;
        int totalRisk = 0;
        Set<Long> riskStudentIds = new HashSet<>();
    }
}
