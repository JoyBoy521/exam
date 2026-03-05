package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.CheatEventReportRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamCheatEvent;
import com.exam.system.entity.Student;
import com.exam.system.mapper.ExamCheatEventMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/student/exams")
public class StudentBehaviorController {

    private final ExamCheatEventMapper examCheatEventMapper;
    private final ExamMapper examMapper;
    private final StudentMapper studentMapper;

    public StudentBehaviorController(ExamCheatEventMapper examCheatEventMapper,
                                     ExamMapper examMapper,
                                     StudentMapper studentMapper) {
        this.examCheatEventMapper = examCheatEventMapper;
        this.examMapper = examMapper;
        this.studentMapper = studentMapper;
    }

    @PostMapping("/{examId}/cheat-events")
    public String reportCheatEvent(@PathVariable Long examId,
                                   @Valid @RequestBody CheatEventReportRequest payload,
                                   HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);

        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        if (exam.getClassId() != null && student.getClassId() != null && !exam.getClassId().equals(student.getClassId())) {
            throw new IllegalArgumentException("无权限上报该考试事件");
        }

        ExamCheatEvent event = new ExamCheatEvent();
        event.setExamId(examId);
        event.setStudentId(studentId);
        event.setType(payload.type());
        event.setDurationSeconds(payload.durationSeconds());
        event.setDetail(payload.detail());
        event.setHappenedAt(LocalDateTime.now());
        examCheatEventMapper.insert(event);
        return "已上报";
    }
}
