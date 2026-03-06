package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.*;
import com.exam.system.mapper.*;
import com.exam.system.service.AuditLogService;
import com.exam.system.service.TeacherExamService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/records")
public class TeacherRecordController {
    private static final Logger log = LoggerFactory.getLogger(TeacherRecordController.class);

    private final ExamRecordMapper examRecordMapper;
    private final ExamRecordAnswerMapper examRecordAnswerMapper;
    private final QuestionMapper questionMapper;
    private final StudentMapper studentMapper;
    private final ExamMapper examMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final TeacherExamService teacherExamService;
    private final AuditLogService auditLogService;

    public TeacherRecordController(ExamRecordMapper examRecordMapper,
                                   ExamRecordAnswerMapper examRecordAnswerMapper,
                                   QuestionMapper questionMapper,
                                   StudentMapper studentMapper,
                                   ExamMapper examMapper,
                                   PaperQuestionMapper paperQuestionMapper,
                                   TeacherExamService teacherExamService,
                                   AuditLogService auditLogService) {
        this.examRecordMapper = examRecordMapper;
        this.examRecordAnswerMapper = examRecordAnswerMapper;
        this.questionMapper = questionMapper;
        this.studentMapper = studentMapper;
        this.examMapper = examMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.teacherExamService = teacherExamService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/exam/{examId}")
    public List<Map<String, Object>> getRecordsByExam(@PathVariable Long examId) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .orderByDesc(ExamRecord::getSubmitTime)
        );

        Set<Long> studentIds = records.stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            Student student = studentMap.get(r.getUserId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("studentNo", student != null ? student.getStudentNo() : String.valueOf(r.getUserId()));
            map.put("studentName", student != null ? student.getName() : ("学生" + r.getUserId()));
            map.put("objectiveScore", r.getObjectiveScore() != null ? r.getObjectiveScore() : 0);
            map.put("subjectiveScore", r.getSubjectiveScore() != null ? r.getSubjectiveScore() : 0);
            map.put("totalScore", r.getTotalScore() != null ? r.getTotalScore() : 0);
            map.put("status", r.getStatus());
            map.put("submitTime", r.getSubmitTime());
            result.add(map);
        }
        return result;
    }

    @PostMapping("/{recordId}/grade")
    public void gradeRecord(@PathVariable Long recordId,
                            @RequestBody Map<String, Object> payload,
                            HttpServletRequest request) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("记录不存在");
        }

        BigDecimal subjectiveScore = new BigDecimal(payload.get("subjectiveScore").toString());
        record.setSubjectiveScore(subjectiveScore);

        BigDecimal objScore = record.getObjectiveScore() != null ? record.getObjectiveScore() : BigDecimal.ZERO;
        record.setTotalScore(objScore.add(subjectiveScore));
        record.setStatus("GRADED");
        examRecordMapper.updateById(record);
        teacherExamService.syncWrongBookForRecord(recordId, record.getUserId());
        log.info("AUDIT grade_record operator={} role={} recordId={} studentId={} subjectiveScore={}",
                CurrentUser.loginName(request), CurrentUser.role(request), recordId, record.getUserId(), subjectiveScore);
        auditLogService.record(
                CurrentUser.userId(request),
                "GRADE_RECORD",
                "EXAM_RECORD",
                recordId,
                "studentId=" + record.getUserId() + ",subjectiveScore=" + subjectiveScore
        );
    }

    @GetMapping("/{recordId}/subjective-answers")
    public List<Map<String, Object>> getSubjectiveAnswers(@PathVariable Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("记录不存在");
        }

        Exam exam = examMapper.selectById(record.getExamId());
        Long paperId = pickAssignedPaperId(exam != null ? exam.getPaperIds() : null, record.getUserId());

        Map<Long, PaperQuestion> scoreMap = paperQuestionMapper.selectList(
                        new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId)
                ).stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, x -> x, (a, b) -> a));

        List<ExamRecordAnswer> answers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, recordId)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecordAnswer ans : answers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && "SHORT_ANSWER".equals(q.getType())) {
                int maxScore = Optional.ofNullable(scoreMap.get(q.getId()))
                        .map(PaperQuestion::getScore)
                        .map(BigDecimal::intValue)
                        .orElse(10);

                Map<String, Object> map = new HashMap<>();
                map.put("answerId", ans.getId());
                map.put("questionId", q.getId());
                map.put("stem", q.getStem());
                map.put("standardAnswer", q.getAnswer());
                map.put("studentAnswer", ans.getUserAnswer());
                map.put("maxScore", maxScore);
                map.put("givenScore", ans.getScore() != null ? ans.getScore().intValue() : 0);
                result.add(map);
            }
        }
        return result;
    }

    private Long pickAssignedPaperId(String paperIdsStr, Long studentId) {
        if (paperIdsStr == null || paperIdsStr.isBlank()) {
            return 1L;
        }
        String[] pIds = paperIdsStr.split(",");
        int idx = Math.floorMod(studentId.intValue(), pIds.length);
        return Long.valueOf(pIds[idx]);
    }
}
