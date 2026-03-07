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

import java.time.LocalDateTime;
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
    public Map<String, Object> getMyRecords(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) Long courseId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);

        LambdaQueryWrapper<ExamRecord> query = new LambdaQueryWrapper<>();
        query.eq(ExamRecord::getUserId, studentId).orderByDesc(ExamRecord::getSubmitTime);
        List<ExamRecord> records = examRecordMapper.selectList(query);
        if (records.isEmpty()) {
            return paginate(List.of(), page, size);
        }
        Map<Long, Exam> examMap = examMapper.selectBatchIds(
                records.stream().map(ExamRecord::getExamId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Exam::getId, x -> x, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamRecord r : records) {
            Exam exam = examMap.get(r.getExamId());
            if (courseId != null && (exam == null || !courseId.equals(exam.getCourseId()))) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("examTitle", exam != null ? exam.getTitle() : "未知考试");
            map.put("totalScore", r.getTotalScore());
            map.put("status", r.getStatus());
            map.put("submitTime", r.getSubmitTime());
            if (!hitRecordFilter(map, status, keyword)) {
                continue;
            }
            rows.add(map);
        }
        return paginate(rows, page, size);
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
        if (wrongAnswers.isEmpty()) {
            return List.of();
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
                wrongAnswers.stream().map(ExamRecordAnswer::getQuestionId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecordAnswer ans : wrongAnswers) {
            Question q = questionMap.get(ans.getQuestionId());
            if (q == null) continue;
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
        boolean canViewReference = "GRADED".equalsIgnoreCase(String.valueOf(record.getStatus()))
                || (exam != null && exam.getEndTime() != null && LocalDateTime.now().isAfter(exam.getEndTime()));
        List<ExamRecordAnswer> answers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, recordId)
        );
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
                answers.stream().map(ExamRecordAnswer::getQuestionId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (ExamRecordAnswer ans : answers) {
            Question q = questionMap.get(ans.getQuestionId());
            if (q == null) continue;
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("questionId", q.getId());
            qMap.put("stem", q.getStem());
            qMap.put("type", q.getType());
            qMap.put("options", q.getOptions());
            qMap.put("studentAnswer", ans.getUserAnswer());
            qMap.put("correctAnswer", canViewReference ? q.getAnswer() : null);
            qMap.put("analysis", canViewReference ? q.getAnalysis() : "考试未结束或未完成阅卷，暂不显示解析");
            qMap.put("score", ans.getScore());
            qMap.put("isCorrect", ans.getIsCorrect());
            detailList.add(qMap);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("examTitle", exam != null ? exam.getTitle() : "未知考试");
        res.put("totalScore", record.getTotalScore());
        res.put("canViewReference", canViewReference);
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
    public List<Exam> listAvailableExams(@RequestParam(required = false) Long courseId,
                                         HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        return teacherExamService.listStudentAvailableExams(studentId, courseId);
    }

    private Map<String, Object> paginate(List<Map<String, Object>> rows, Integer page, Integer size) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.max(1, Math.min(size == null ? 10 : size, 200));
        int total = rows.size();
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows.subList(fromIndex, toIndex));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private boolean hitRecordFilter(Map<String, Object> row, String status, String keyword) {
        if (status != null && !status.isBlank()) {
            if (!status.trim().equalsIgnoreCase(String.valueOf(row.getOrDefault("status", "")))) {
                return false;
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            return String.valueOf(row.getOrDefault("examTitle", "")).toLowerCase().contains(kw);
        }
        return true;
    }
}
