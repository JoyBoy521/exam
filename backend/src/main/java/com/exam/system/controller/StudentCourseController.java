package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Course;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.ExamRecordAnswer;
import com.exam.system.entity.PaperQuestion;
import com.exam.system.entity.Question;
import com.exam.system.entity.Student;
import com.exam.system.entity.StudentWrongBook;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordAnswerMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.PaperQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.mapper.StudentWrongBookMapper;
import com.exam.system.service.TeacherExamService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/courses")
public class StudentCourseController {

    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamRecordAnswerMapper examRecordAnswerMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final StudentWrongBookMapper studentWrongBookMapper;
    private final QuestionMapper questionMapper;
    private final TeacherExamService teacherExamService;

    public StudentCourseController(StudentMapper studentMapper,
                                   CourseMapper courseMapper,
                                   ExamMapper examMapper,
                                   ExamRecordMapper examRecordMapper,
                                   ExamRecordAnswerMapper examRecordAnswerMapper,
                                   PaperQuestionMapper paperQuestionMapper,
                                   StudentWrongBookMapper studentWrongBookMapper,
                                   QuestionMapper questionMapper,
                                   TeacherExamService teacherExamService) {
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.examRecordAnswerMapper = examRecordAnswerMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.studentWrongBookMapper = studentWrongBookMapper;
        this.questionMapper = questionMapper;
        this.teacherExamService = teacherExamService;
    }

    @GetMapping
    public List<Map<String, Object>> myCourses(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }

        List<Exam> allStudentExams = teacherExamService.listStudentAvailableExams(studentId);
        Set<Long> courseIdsFromExam = allStudentExams.stream()
                .map(Exam::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Course> candidateCourses = loadCourseCandidates(courseIdsFromExam);
        Map<Long, List<Exam>> examsByCourse = allStudentExams.stream()
                .filter(x -> x.getCourseId() != null)
                .collect(Collectors.groupingBy(Exam::getCourseId));

        Set<Long> studentWrongQuestionIds = studentWrongBookMapper.selectList(
                        new LambdaQueryWrapper<StudentWrongBook>().eq(StudentWrongBook::getStudentId, studentId)
                ).stream()
                .map(StudentWrongBook::getQuestionId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course c : candidateCourses) {
            List<Exam> exams = examsByCourse.getOrDefault(c.getId(), List.of());
            Set<Long> courseQuestionIds = questionIdsFromExams(exams);
            long wrongCount = courseQuestionIds.isEmpty()
                    ? 0
                    : studentWrongQuestionIds.stream().filter(courseQuestionIds::contains).count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("title", c.getTitle());
            row.put("description", c.getDescription());
            row.put("coverUrl", c.getCoverUrl());
            row.put("status", c.getStatus());
            row.put("ongoingExamCount", exams.stream().filter(x -> "ONGOING".equals(x.getStatus())).count());
            row.put("upcomingExamCount", exams.stream().filter(x -> "NOT_STARTED".equals(x.getStatus())).count());
            row.put("finishedExamCount", exams.stream().filter(x -> "FINISHED".equals(x.getStatus())).count());
            row.put("wrongCount", wrongCount);
            rows.add(row);
        }
        return rows;
    }

    @GetMapping("/{courseId}/overview")
    public Map<String, Object> courseOverview(@PathVariable Long courseId, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }

        List<Exam> exams = teacherExamService.listStudentAvailableExams(studentId, courseId);
        Set<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
        Set<Long> questionIds = questionIdsFromExams(exams);

        List<ExamRecord> records = examIds.isEmpty()
                ? List.of()
                : examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, studentId)
                .in(ExamRecord::getExamId, examIds)
                .orderByDesc(ExamRecord::getSubmitTime));

        List<StudentWrongBook> wrongBooks = questionIds.isEmpty()
                ? List.of()
                : studentWrongBookMapper.selectList(new LambdaQueryWrapper<StudentWrongBook>()
                .eq(StudentWrongBook::getStudentId, studentId)
                .in(StudentWrongBook::getQuestionId, questionIds)
                .orderByDesc(StudentWrongBook::getCreateTime));

        Map<Long, Question> qMap = wrongBooks.isEmpty()
                ? Map.of()
                : questionMapper.selectBatchIds(
                wrongBooks.stream().map(StudentWrongBook::getQuestionId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        List<Map<String, Object>> wrongPreview = wrongBooks.stream().limit(10).map(wb -> {
            Map<String, Object> x = new LinkedHashMap<>();
            Question q = qMap.get(wb.getQuestionId());
            x.put("id", wb.getId());
            x.put("questionId", wb.getQuestionId());
            x.put("stem", q == null ? ("题目#" + wb.getQuestionId()) : q.getStem());
            x.put("type", q == null ? "" : q.getType());
            x.put("masteryLevel", wb.getMasteryLevel());
            x.put("practiceCount", safeInt(wb.getPracticeCount()));
            x.put("correctCount", safeInt(wb.getCorrectCount()));
            x.put("accuracy", computeAccuracy(wb));
            return x;
        }).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("courseId", course.getId());
        summary.put("courseTitle", course.getTitle());
        summary.put("courseDescription", course.getDescription());
        summary.put("courseCoverUrl", course.getCoverUrl());
        summary.put("examCount", exams.size());
        summary.put("ongoingExamCount", exams.stream().filter(x -> "ONGOING".equals(x.getStatus())).count());
        summary.put("upcomingExamCount", exams.stream().filter(x -> "NOT_STARTED".equals(x.getStatus())).count());
        summary.put("finishedExamCount", exams.stream().filter(x -> "FINISHED".equals(x.getStatus())).count());
        summary.put("recordCount", records.size());
        summary.put("wrongCount", wrongBooks.size());
        summary.put("practiceCount", wrongBooks.stream().mapToInt(x -> safeInt(x.getPracticeCount())).sum());
        summary.put("masteredCount", wrongBooks.stream().filter(x -> "MASTERED".equals(x.getMasteryLevel())).count());
        summary.put("needReviewCount", wrongBooks.stream().filter(x -> "NEEDS_REVIEW".equals(x.getMasteryLevel())).count());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("exams", exams);
        result.put("recentRecords", records.stream().limit(10).toList());
        result.put("wrongPreview", wrongPreview);
        return result;
    }

    private List<Course> loadCourseCandidates(Set<Long> courseIdsFromExam) {
        if (!courseIdsFromExam.isEmpty()) {
            return courseMapper.selectBatchIds(courseIdsFromExam);
        }
        List<Course> active = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, "ACTIVE").orderByDesc(Course::getUpdatedAt)
        );
        if (!active.isEmpty()) {
            return active;
        }
        return courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByDesc(Course::getUpdatedAt));
    }

    private Set<Long> questionIdsFromExams(Collection<Exam> exams) {
        Set<Long> paperIds = new HashSet<>();
        for (Exam exam : exams) {
            if (exam.getPaperIds() == null || exam.getPaperIds().isBlank()) {
                continue;
            }
            String[] parts = exam.getPaperIds().split(",");
            for (String p : parts) {
                String t = p.trim();
                if (t.isEmpty()) {
                    continue;
                }
                try {
                    paperIds.add(Long.valueOf(t));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (paperIds.isEmpty()) {
            return Set.of();
        }
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<PaperQuestion>()
                        .in(PaperQuestion::getPaperId, paperIds)
        );
        if (paperQuestions.isEmpty()) {
            return Set.of();
        }
        return paperQuestions.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toSet());
    }

    private int safeInt(Integer n) {
        return n == null ? 0 : n;
    }

    private double computeAccuracy(StudentWrongBook wb) {
        int practice = safeInt(wb.getPracticeCount());
        int correct = safeInt(wb.getCorrectCount());
        if (practice <= 0) {
            return 0.0;
        }
        return Math.round((correct * 10000.0 / practice)) / 100.0;
    }
}
