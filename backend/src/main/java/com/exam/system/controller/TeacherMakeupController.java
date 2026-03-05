package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.ReviewMakeupRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.Student;
import com.exam.system.entity.StudentMakeupRequest;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.StudentMakeupRequestMapper;
import com.exam.system.mapper.StudentMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/teacher/makeup-requests")
public class TeacherMakeupController {

    private final StudentMakeupRequestMapper studentMakeupRequestMapper;
    private final StudentMapper studentMapper;
    private final ExamMapper examMapper;

    public TeacherMakeupController(StudentMakeupRequestMapper studentMakeupRequestMapper,
                                   StudentMapper studentMapper,
                                   ExamMapper examMapper) {
        this.studentMakeupRequestMapper = studentMakeupRequestMapper;
        this.studentMapper = studentMapper;
        this.examMapper = examMapper;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<StudentMakeupRequest> query = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            query.eq(StudentMakeupRequest::getStatus, status);
        }
        query.orderByDesc(StudentMakeupRequest::getRequestedAt);

        List<StudentMakeupRequest> requests = studentMakeupRequestMapper.selectList(query);
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentMakeupRequest request : requests) {
            Student student = studentMapper.selectById(request.getStudentId());
            Exam exam = examMapper.selectById(request.getExamId());
            Map<String, Object> row = new HashMap<>();
            row.put("id", request.getId());
            row.put("examId", request.getExamId());
            row.put("examTitle", exam != null ? exam.getTitle() : "未知考试");
            row.put("studentId", request.getStudentId());
            row.put("studentNo", student != null ? student.getStudentNo() : String.valueOf(request.getStudentId()));
            row.put("studentName", student != null ? student.getName() : ("学生" + request.getStudentId()));
            row.put("reason", request.getReason());
            row.put("status", request.getStatus());
            row.put("teacherComment", request.getTeacherComment());
            row.put("approvedExtraMinutes", request.getApprovedExtraMinutes());
            row.put("requestedAt", request.getRequestedAt());
            row.put("reviewedAt", request.getReviewedAt());
            result.add(row);
        }
        return result;
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id, @Valid @RequestBody ReviewMakeupRequest payload) {
        StudentMakeupRequest request = studentMakeupRequestMapper.selectById(id);
        if (request == null) {
            throw new IllegalArgumentException("补考申请不存在");
        }

        String status = payload.status().toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("状态必须是 APPROVED 或 REJECTED");
        }

        request.setStatus(status);
        request.setTeacherComment(payload.teacherComment());
        request.setApprovedExtraMinutes("APPROVED".equals(status) ? payload.approvedExtraMinutes() : null);
        request.setReviewedAt(LocalDateTime.now());
        studentMakeupRequestMapper.updateById(request);
        return "审核完成";
    }
}
