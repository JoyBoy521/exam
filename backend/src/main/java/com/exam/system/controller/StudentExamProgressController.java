package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamAnswerDraft;
import com.exam.system.entity.ExamHeartbeat;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.Student;
import com.exam.system.mapper.ExamAnswerDraftMapper;
import com.exam.system.mapper.ExamHeartbeatMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/exams")
public class StudentExamProgressController {

    private final ExamMapper examMapper;
    private final StudentMapper studentMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerDraftMapper examAnswerDraftMapper;
    private final ExamHeartbeatMapper examHeartbeatMapper;

    public StudentExamProgressController(ExamMapper examMapper,
                                         StudentMapper studentMapper,
                                         ExamRecordMapper examRecordMapper,
                                         ExamAnswerDraftMapper examAnswerDraftMapper,
                                         ExamHeartbeatMapper examHeartbeatMapper) {
        this.examMapper = examMapper;
        this.studentMapper = studentMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerDraftMapper = examAnswerDraftMapper;
        this.examHeartbeatMapper = examHeartbeatMapper;
    }

    @PostMapping("/{examId}/progress/save")
    public Map<String, Object> saveProgress(@PathVariable Long examId,
                                            @RequestBody Map<String, Object> payload,
                                            HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        validateStudentExam(examId, studentId);
        ensureNotSubmitted(examId, studentId);

        Object answersRaw = payload.get("answers");
        if (!(answersRaw instanceof Map<?, ?> answers)) {
            throw new IllegalArgumentException("answers 参数错误");
        }

        for (Map.Entry<?, ?> entry : answers.entrySet()) {
            String qidText = String.valueOf(entry.getKey());
            if (qidText.isBlank()) {
                continue;
            }
            Long questionId = Long.valueOf(qidText);
            String userAnswer = String.valueOf(entry.getValue() == null ? "" : entry.getValue()).trim();

            ExamAnswerDraft draft = examAnswerDraftMapper.selectOne(
                    new LambdaQueryWrapper<ExamAnswerDraft>()
                            .eq(ExamAnswerDraft::getExamId, examId)
                            .eq(ExamAnswerDraft::getStudentId, studentId)
                            .eq(ExamAnswerDraft::getQuestionId, questionId)
                            .last("limit 1")
            );
            if (draft == null) {
                draft = new ExamAnswerDraft();
                draft.setExamId(examId);
                draft.setStudentId(studentId);
                draft.setQuestionId(questionId);
                draft.setMarkedFlag(0);
            }
            draft.setUserAnswer(userAnswer);
            draft.setUpdatedAt(LocalDateTime.now());
            if (draft.getId() == null) {
                examAnswerDraftMapper.insert(draft);
            } else {
                examAnswerDraftMapper.updateById(draft);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("savedAt", LocalDateTime.now());
        result.put("savedCount", answers.size());
        return result;
    }

    @GetMapping("/{examId}/progress/load")
    public Map<String, Object> loadProgress(@PathVariable Long examId, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        validateStudentExam(examId, studentId);

        List<ExamAnswerDraft> drafts = examAnswerDraftMapper.selectList(
                new LambdaQueryWrapper<ExamAnswerDraft>()
                        .eq(ExamAnswerDraft::getExamId, examId)
                        .eq(ExamAnswerDraft::getStudentId, studentId)
        );
        Map<String, String> answers = new HashMap<>();
        LocalDateTime latest = null;
        for (ExamAnswerDraft draft : drafts) {
            answers.put(String.valueOf(draft.getQuestionId()), draft.getUserAnswer() == null ? "" : draft.getUserAnswer());
            if (latest == null || (draft.getUpdatedAt() != null && draft.getUpdatedAt().isAfter(latest))) {
                latest = draft.getUpdatedAt();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("answers", answers);
        result.put("savedAt", latest);
        return result;
    }

    @PostMapping("/{examId}/heartbeat")
    public String heartbeat(@PathVariable Long examId,
                            @RequestBody Map<String, Object> payload,
                            HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        validateStudentExam(examId, studentId);

        Integer answeredCount = parseInteger(payload.get("answeredCount"), 0);
        Integer totalCount = parseInteger(payload.get("totalCount"), 0);
        Integer timeLeftSeconds = parseInteger(payload.get("timeLeftSeconds"), 0);

        ExamHeartbeat heartbeat = examHeartbeatMapper.selectOne(
                new LambdaQueryWrapper<ExamHeartbeat>()
                        .eq(ExamHeartbeat::getExamId, examId)
                        .eq(ExamHeartbeat::getStudentId, studentId)
                        .last("limit 1")
        );
        if (heartbeat == null) {
            heartbeat = new ExamHeartbeat();
            heartbeat.setExamId(examId);
            heartbeat.setStudentId(studentId);
        }
        heartbeat.setAnsweredCount(Math.max(0, answeredCount));
        heartbeat.setTotalCount(Math.max(0, totalCount));
        heartbeat.setTimeLeftSeconds(Math.max(0, timeLeftSeconds));
        heartbeat.setLastActiveAt(LocalDateTime.now());
        heartbeat.setUpdatedAt(LocalDateTime.now());

        if (heartbeat.getId() == null) {
            examHeartbeatMapper.insert(heartbeat);
        } else {
            examHeartbeatMapper.updateById(heartbeat);
        }
        return "ok";
    }

    private void ensureNotSubmitted(Long examId, Long studentId) {
        long submitted = examRecordMapper.selectCount(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .eq(ExamRecord::getUserId, studentId)
        );
        if (submitted > 0) {
            throw new IllegalArgumentException("该考试已交卷，无法保存草稿");
        }
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

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }
}
