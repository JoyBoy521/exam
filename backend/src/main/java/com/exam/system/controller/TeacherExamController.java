package com.exam.system.controller;

import com.exam.system.dto.*;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ParallelAssignment;
import com.exam.system.entity.ParallelGroup;
import com.exam.system.service.TeacherExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TeacherExamController {
    private final TeacherExamService service;

    public TeacherExamController(TeacherExamService service) {
        this.service = service;
    }

    @PostMapping("/teacher/exams")
    public Exam createExam(@RequestBody Map<String, Object> payload) {
        return service.createExam(payload);
    }

    @GetMapping("/teacher/exams")
    public List<Exam> listTeacherExams(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long classId,
                                       @RequestParam(required = false) Long courseId) {
        return service.listExams(keyword, status, classId, courseId);
    }

    @PostMapping("/teacher/exams/{examId}/revoke")
    public String revokeExam(@PathVariable Long examId) {
        service.revokeExam(examId);
        return "考试已撤回";
    }

    @GetMapping("/teacher/exams/{examId}/submissions")
    public List<TeacherSubmissionViewResponse> listExamSubmissions(@PathVariable Long examId) {
        return service.listExamSubmissions(examId);
    }

    @PostMapping("/teacher/parallel-groups")
    public ParallelGroup createParallelGroup(@Valid @RequestBody CreateParallelGroupRequest request) {
        return (ParallelGroup) service.createParallelGroup(request);
    }

    @GetMapping("/teacher/parallel-groups")
    public List<Object> listParallelGroups() {
        return service.listParallelGroups();
    }

    @PostMapping("/teacher/parallel-groups/{groupId}/assign")
    public List<Object> assign(@PathVariable Long groupId, @Valid @RequestBody AssignParallelRequest request) {
        return service.assignParallelPapers(groupId, request);
    }

    @GetMapping("/teacher/assignments")
    public List<Object> assignments() {
        return service.listAssignments();
    }

    @GetMapping("/teacher/statistics")
    public StatisticsResponse statistics() {
        return service.statistics();
    }

    @GetMapping("/teacher/todo")
    public TeacherTodoResponse todo() {
        return service.teacherTodo();
    }
}
