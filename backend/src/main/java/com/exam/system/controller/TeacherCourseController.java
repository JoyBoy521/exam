package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.ClassInfo;
import com.exam.system.entity.Course;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamCheatEvent;
import com.exam.system.entity.ExamRecord;
import com.exam.system.mapper.ClassInfoMapper;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.ExamCheatEventMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/courses")
public class TeacherCourseController {

    private final CourseMapper courseMapper;
    private final ExamMapper examMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamCheatEventMapper examCheatEventMapper;

    public TeacherCourseController(CourseMapper courseMapper,
                                   ExamMapper examMapper,
                                   ClassInfoMapper classInfoMapper,
                                   ExamRecordMapper examRecordMapper,
                                   ExamCheatEventMapper examCheatEventMapper) {
        this.courseMapper = courseMapper;
        this.examMapper = examMapper;
        this.classInfoMapper = classInfoMapper;
        this.examRecordMapper = examRecordMapper;
        this.examCheatEventMapper = examCheatEventMapper;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        try {
            List<Course> courses = courseMapper.selectList(
                    new LambdaQueryWrapper<Course>().orderByDesc(Course::getUpdatedAt)
            );
            if (courses.isEmpty()) {
                return List.of();
            }

            Map<Long, List<Exam>> examsByCourse = examMapper.selectList(
                    new LambdaQueryWrapper<Exam>().in(Exam::getCourseId, courses.stream().map(Course::getId).toList())
            ).stream().collect(Collectors.groupingBy(Exam::getCourseId));

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Course c : courses) {
                List<Exam> exams = examsByCourse.getOrDefault(c.getId(), List.of());
                Set<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
                Set<Long> classIds = exams.stream().map(Exam::getClassId).filter(x -> x != null).collect(Collectors.toSet());

                int studentCount = classIds.isEmpty()
                        ? 0
                        : classInfoMapper.selectBatchIds(classIds).stream().mapToInt(x -> x.getStudentCount() == null ? 0 : x.getStudentCount()).sum();
                long pendingMarking = examIds.isEmpty()
                        ? 0
                        : examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .in(ExamRecord::getExamId, examIds)
                        .eq(ExamRecord::getStatus, "MARKING"));
                long riskEvents7d = examIds.isEmpty()
                        ? 0
                        : examCheatEventMapper.selectCount(new LambdaQueryWrapper<ExamCheatEvent>()
                        .in(ExamCheatEvent::getExamId, examIds)
                        .ge(ExamCheatEvent::getHappenedAt, LocalDateTime.now().minusDays(7)));

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getId());
                row.put("title", c.getTitle());
                row.put("description", c.getDescription());
                row.put("status", c.getStatus());
                row.put("coverUrl", c.getCoverUrl());
                row.put("examCount", exams.size());
                row.put("ongoingExamCount", exams.stream().filter(x -> "ONGOING".equals(x.getStatus())).count());
                row.put("studentCount", studentCount);
                row.put("pendingMarkingCount", pendingMarking);
                row.put("riskEvents7d", riskEvents7d);
                row.put("classCount", classIds.size());
                rows.add(row);
            }
            return rows;
        } catch (Exception ex) {
            return List.of();
        }
    }
}
