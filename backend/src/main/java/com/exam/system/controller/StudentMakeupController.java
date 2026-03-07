package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.CreateMakeupRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.StudentMakeupRequest;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.StudentMakeupRequestMapper;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/makeup-requests")
public class StudentMakeupController {

    private final StudentMakeupRequestMapper studentMakeupRequestMapper;
    private final ExamMapper examMapper;

    public StudentMakeupController(StudentMakeupRequestMapper studentMakeupRequestMapper, ExamMapper examMapper) {
        this.studentMakeupRequestMapper = studentMakeupRequestMapper;
        this.examMapper = examMapper;
    }

    @PostMapping
    public String create(@Valid @RequestBody CreateMakeupRequest request, HttpServletRequest httpServletRequest) {
        Long studentId = CurrentUser.userId(httpServletRequest);

        Exam exam = examMapper.selectById(request.examId());
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        Long pendingCount = studentMakeupRequestMapper.selectCount(
                new LambdaQueryWrapper<StudentMakeupRequest>()
                        .eq(StudentMakeupRequest::getStudentId, studentId)
                        .eq(StudentMakeupRequest::getExamId, request.examId())
                        .eq(StudentMakeupRequest::getStatus, "PENDING")
        );
        if (pendingCount != null && pendingCount > 0) {
            throw new IllegalArgumentException("该考试已有待审核补考申请，请勿重复提交");
        }

        StudentMakeupRequest entity = new StudentMakeupRequest();
        entity.setExamId(request.examId());
        entity.setStudentId(studentId);
        entity.setReason(request.reason());
        entity.setStatus("PENDING");
        entity.setRequestedAt(LocalDateTime.now());
        studentMakeupRequestMapper.insert(entity);
        return "补考申请已提交";
    }

    @GetMapping
    public Map<String, Object> myList(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String keyword,
                                      HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        List<StudentMakeupRequest> rows = studentMakeupRequestMapper.selectList(
                new LambdaQueryWrapper<StudentMakeupRequest>()
                        .eq(StudentMakeupRequest::getStudentId, studentId)
                        .orderByDesc(StudentMakeupRequest::getRequestedAt)
        );
        if (rows.isEmpty()) {
            return paginate(List.of(), page, size);
        }
        Set<Long> examIds = rows.stream().map(StudentMakeupRequest::getExamId).collect(Collectors.toSet());
        Map<Long, String> examTitleMap = examIds.isEmpty()
                ? Map.of()
                : examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, Exam::getTitle, (a, b) -> a));
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (StudentMakeupRequest x : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", x.getId());
            item.put("examId", x.getExamId());
            item.put("examTitle", examTitleMap.getOrDefault(x.getExamId(), "未知考试"));
            item.put("status", x.getStatus());
            item.put("reason", x.getReason());
            item.put("teacherComment", x.getTeacherComment());
            item.put("approvedExtraMinutes", x.getApprovedExtraMinutes());
            item.put("requestedAt", x.getRequestedAt());
            item.put("reviewedAt", x.getReviewedAt());
            if (!hitFilter(item, status, keyword)) {
                continue;
            }
            mapped.add(item);
        }
        return paginate(mapped, page, size);
    }

    @DeleteMapping("/{id}")
    public String cancel(@PathVariable Long id, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        StudentMakeupRequest row = studentMakeupRequestMapper.selectById(id);
        if (row == null || !studentId.equals(row.getStudentId())) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!"PENDING".equals(row.getStatus())) {
            throw new IllegalArgumentException("只有待审核申请可以撤回");
        }
        row.setStatus("CANCELED");
        row.setReviewedAt(LocalDateTime.now());
        row.setTeacherComment("学生主动撤回");
        studentMakeupRequestMapper.updateById(row);
        return "已撤回申请";
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        List<StudentMakeupRequest> rows = studentMakeupRequestMapper.selectList(
                new LambdaQueryWrapper<StudentMakeupRequest>()
                        .eq(StudentMakeupRequest::getStudentId, studentId)
                        .orderByDesc(StudentMakeupRequest::getRequestedAt)
        );
        long pendingCount = rows.stream().filter(x -> "PENDING".equals(x.getStatus())).count();
        long approvedCount = rows.stream().filter(x -> "APPROVED".equals(x.getStatus())).count();
        long rejectedCount = rows.stream().filter(x -> "REJECTED".equals(x.getStatus())).count();
        long canceledCount = rows.stream().filter(x -> "CANCELED".equals(x.getStatus())).count();

        Set<Long> examIds = rows.stream().map(StudentMakeupRequest::getExamId).collect(Collectors.toSet());
        Map<Long, String> examTitleMap = examIds.isEmpty()
                ? Map.of()
                : examMapper.selectBatchIds(examIds).stream().collect(Collectors.toMap(Exam::getId, Exam::getTitle, (a, b) -> a));

        List<Map<String, Object>> latest = rows.stream().limit(5).map(x -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", x.getId());
            item.put("examId", x.getExamId());
            item.put("examTitle", examTitleMap.getOrDefault(x.getExamId(), "未知考试"));
            item.put("status", x.getStatus());
            item.put("reason", x.getReason());
            item.put("teacherComment", x.getTeacherComment());
            item.put("approvedExtraMinutes", x.getApprovedExtraMinutes());
            item.put("requestedAt", x.getRequestedAt());
            item.put("reviewedAt", x.getReviewedAt());
            return item;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("pendingCount", pendingCount);
        result.put("approvedCount", approvedCount);
        result.put("rejectedCount", rejectedCount);
        result.put("canceledCount", canceledCount);
        result.put("latest", latest);
        return result;
    }

    private boolean hitFilter(Map<String, Object> row, String status, String keyword) {
        if (status != null && !status.isBlank() && !status.trim().equalsIgnoreCase(String.valueOf(row.getOrDefault("status", "")))) {
            return false;
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            return String.valueOf(row.getOrDefault("examTitle", "")).toLowerCase().contains(kw)
                    || String.valueOf(row.getOrDefault("reason", "")).toLowerCase().contains(kw)
                    || String.valueOf(row.getOrDefault("teacherComment", "")).toLowerCase().contains(kw);
        }
        return true;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> rows, Integer page, Integer size) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.max(1, Math.min(size == null ? 10 : size, 200));
        int total = rows.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }
}
