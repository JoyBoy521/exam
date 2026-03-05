package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.StudentExamDetailResponse;
import com.exam.system.dto.StudentSubmitRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.ExamRecordAnswer;
import com.exam.system.entity.Question;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordAnswerMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.service.TeacherExamService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {

    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamRecordAnswerMapper examRecordAnswerMapper;
    private final TeacherExamService teacherExamService;

    public StudentExamController(ExamMapper examMapper,
                                 QuestionMapper questionMapper,
                                 ExamRecordMapper examRecordMapper,
                                 ExamRecordAnswerMapper examRecordAnswerMapper,
                                 TeacherExamService teacherExamService) {
        this.examMapper = examMapper;
        this.questionMapper = questionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examRecordAnswerMapper = examRecordAnswerMapper;
        this.teacherExamService = teacherExamService;
    }

    @GetMapping("/my-records")
    public List<Map<String, Object>> getMyRecords(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);

        LambdaQueryWrapper<ExamRecord> query = new LambdaQueryWrapper<>();
        query.eq(ExamRecord::getUserId, studentId).orderByDesc(ExamRecord::getSubmitTime);
        List<ExamRecord> records = examRecordMapper.selectList(query);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            Exam exam = examMapper.selectById(r.getExamId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("examTitle", exam != null ? exam.getTitle() : "未知考试");
            map.put("totalScore", r.getTotalScore());
            map.put("status", r.getStatus());
            map.put("submitTime", r.getSubmitTime());
            result.add(map);
        }
        return result;
    }

    @GetMapping("/wrong-questions")
    public List<Map<String, Object>> getWrongQuestions(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);

        List<Long> myRecordIds = examRecordMapper.selectList(
                        new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getUserId, studentId)
                ).stream()
                .map(ExamRecord::getId)
                .toList();

        if (myRecordIds.isEmpty()) {
            return List.of();
        }

        List<ExamRecordAnswer> wrongAnswers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>()
                        .in(ExamRecordAnswer::getRecordId, myRecordIds)
                        .eq(ExamRecordAnswer::getIsCorrect, 0)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecordAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q == null) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("answerId", ans.getId());
            map.put("questionId", q.getId());
            map.put("stem", q.getStem());
            map.put("type", q.getType());
            map.put("options", q.getOptions());
            map.put("studentAnswer", ans.getUserAnswer());
            map.put("correctAnswer", q.getAnswer());
            map.put("analysis", q.getAnalysis());
            result.add(map);
        }
        return result;
    }

    @GetMapping("/records/{recordId}/detail")
    public Map<String, Object> getRecordDetail(@PathVariable Long recordId, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);

        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !Objects.equals(record.getUserId(), studentId)) {
            throw new IllegalArgumentException("无权限访问该记录");
        }

        Exam exam = examMapper.selectById(record.getExamId());
        List<ExamRecordAnswer> answers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, recordId)
        );

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (ExamRecordAnswer ans : answers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q == null) {
                continue;
            }
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("questionId", q.getId());
            qMap.put("stem", q.getStem());
            qMap.put("type", q.getType());
            qMap.put("options", q.getOptions());
            qMap.put("studentAnswer", ans.getUserAnswer());
            qMap.put("correctAnswer", q.getAnswer());
            qMap.put("analysis", q.getAnalysis());
            qMap.put("score", ans.getScore());
            qMap.put("isCorrect", ans.getIsCorrect());
            detailList.add(qMap);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("examTitle", exam != null ? exam.getTitle() : "未知考试");
        res.put("totalScore", record.getTotalScore());
        res.put("details", detailList);
        return res;
    }

    @GetMapping("/detail/{examId}")
    public StudentExamDetailResponse examDetail(@PathVariable Long examId, HttpServletRequest request) {
        return teacherExamService.getStudentExamDetail(examId, CurrentUser.userId(request));
    }

    @PostMapping("/{examId}/submit")
    public String submitExam(@PathVariable Long examId,
                             @Valid @RequestBody StudentSubmitRequest request,
                             HttpServletRequest httpRequest) {
        Long studentId = CurrentUser.userId(httpRequest);
        teacherExamService.submitStudentExam(examId, studentId, request.answers());
        return "交卷成功！";
    }

    @GetMapping("/list")
    public List<Exam> listAvailableExams(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        return teacherExamService.listStudentAvailableExams(studentId);
    }
}
