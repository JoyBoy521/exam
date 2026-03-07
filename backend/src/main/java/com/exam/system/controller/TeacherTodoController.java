package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamCheatEvent;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.StudentMakeupRequest;
import com.exam.system.mapper.ExamCheatEventMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.StudentMakeupRequestMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/todo")
public class TeacherTodoController {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final StudentMakeupRequestMapper makeupRequestMapper;
    private final ExamCheatEventMapper cheatEventMapper;

    public TeacherTodoController(ExamMapper examMapper,
                                 ExamRecordMapper examRecordMapper,
                                 StudentMakeupRequestMapper makeupRequestMapper,
                                 ExamCheatEventMapper cheatEventMapper) {
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.makeupRequestMapper = makeupRequestMapper;
        this.cheatEventMapper = cheatEventMapper;
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> tasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();

        long pendingManual = examRecordMapper.selectCount(
                new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getStatus, "MARKING")
        );
        if (pendingManual > 0) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "MARKING");
            row.put("title", "待批阅主观题");
            row.put("description", "有 " + pendingManual + " 份答卷待人工批阅");
            row.put("count", pendingManual);
            row.put("route", "/exam");
            tasks.add(row);
        }

        long pendingMakeup = makeupRequestMapper.selectCount(
                new LambdaQueryWrapper<StudentMakeupRequest>().eq(StudentMakeupRequest::getStatus, "PENDING")
        );
        if (pendingMakeup > 0) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "MAKEUP");
            row.put("title", "待审核补考申请");
            row.put("description", "有 " + pendingMakeup + " 条补考申请待处理");
            row.put("count", pendingMakeup);
            row.put("route", "/manage");
            tasks.add(row);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime within24h = now.plusHours(24);
        long upcoming = examMapper.selectCount(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getStatus, "NOT_STARTED")
                        .ge(Exam::getStartTime, now)
                        .le(Exam::getStartTime, within24h)
        );
        if (upcoming > 0) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "UPCOMING_EXAM");
            row.put("title", "即将开考");
            row.put("description", "24小时内有 " + upcoming + " 场考试将开始");
            row.put("count", upcoming);
            row.put("route", "/exam");
            tasks.add(row);
        }

        LocalDateTime latestRiskWindow = now.minusHours(2);
        long riskEvents = cheatEventMapper.selectCount(
                new LambdaQueryWrapper<ExamCheatEvent>().ge(ExamCheatEvent::getHappenedAt, latestRiskWindow)
        );
        if (riskEvents > 0) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "RISK");
            row.put("title", "近期异常风险");
            row.put("description", "最近2小时内触发 " + riskEvents + " 条风险事件");
            row.put("count", riskEvents);
            row.put("route", "/monitor");
            tasks.add(row);
        }

        tasks.sort((a, b) -> Integer.compare(
                Integer.parseInt(String.valueOf(b.getOrDefault("count", 0))),
                Integer.parseInt(String.valueOf(a.getOrDefault("count", 0)))
        ));
        return tasks;
    }
}
