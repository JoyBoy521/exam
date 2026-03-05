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
import java.util.List;

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
    public List<StudentMakeupRequest> myList(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        return studentMakeupRequestMapper.selectList(
                new LambdaQueryWrapper<StudentMakeupRequest>()
                        .eq(StudentMakeupRequest::getStudentId, studentId)
                        .orderByDesc(StudentMakeupRequest::getRequestedAt)
        );
    }
}
