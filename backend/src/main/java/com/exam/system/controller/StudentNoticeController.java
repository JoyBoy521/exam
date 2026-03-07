package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.Student;
import com.exam.system.entity.StudentMakeupRequest;
import com.exam.system.entity.StudentNoticeRead;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.StudentMakeupRequestMapper;
import com.exam.system.mapper.StudentNoticeReadMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/notices")
public class StudentNoticeController {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final StudentMakeupRequestMapper makeupRequestMapper;
    private final StudentMapper studentMapper;
    private final StudentNoticeReadMapper noticeReadMapper;

    public StudentNoticeController(ExamMapper examMapper,
                                   ExamRecordMapper examRecordMapper,
                                   StudentMakeupRequestMapper makeupRequestMapper,
                                   StudentMapper studentMapper,
                                   StudentNoticeReadMapper noticeReadMapper) {
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.makeupRequestMapper = makeupRequestMapper;
        this.studentMapper = studentMapper;
        this.noticeReadMapper = noticeReadMapper;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return paginate(List.of(), page, size);
        }

        List<Map<String, Object>> notices = new ArrayList<>();
        appendUpcomingExamNotices(notices, student);
        appendGradedNotices(notices, studentId);
        appendMakeupReviewNotices(notices, studentId);

        notices.sort((a, b) -> {
            LocalDateTime bt = (LocalDateTime) b.get("time");
            LocalDateTime at = (LocalDateTime) a.get("time");
            if (bt == null && at == null) return 0;
            if (bt == null) return -1;
            if (at == null) return 1;
            return bt.compareTo(at);
        });
        Set<String> noticeIds = notices.stream()
                .map(x -> String.valueOf(x.get("noticeId")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> readSet = loadReadSet(studentId, noticeIds);
        int unreadCount = 0;
        for (Map<String, Object> notice : notices) {
            String noticeId = String.valueOf(notice.get("noticeId"));
            boolean isRead = readSet.contains(noticeId);
            notice.put("isRead", isRead);
            if (!isRead) {
                unreadCount++;
            }
        }

        Map<String, Object> result = paginate(notices, page, size);
        result.put("unreadCount", unreadCount);
        return result;
    }

    @PostMapping("/read")
    public String markRead(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Object idsRaw = payload.get("noticeIds");
        if (!(idsRaw instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("noticeIds 不能为空");
        }
        Set<String> noticeIds = list.stream().map(String::valueOf).map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.toSet());
        if (noticeIds.isEmpty()) {
            throw new IllegalArgumentException("noticeIds 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        for (String noticeId : noticeIds) {
            StudentNoticeRead existed = noticeReadMapper.selectOne(
                    new LambdaQueryWrapper<StudentNoticeRead>()
                            .eq(StudentNoticeRead::getStudentId, studentId)
                            .eq(StudentNoticeRead::getNoticeId, noticeId)
                            .last("limit 1")
            );
            if (existed != null) {
                continue;
            }
            StudentNoticeRead row = new StudentNoticeRead();
            row.setStudentId(studentId);
            row.setNoticeId(noticeId);
            row.setReadAt(now);
            noticeReadMapper.insert(row);
        }
        return "已标记已读";
    }

    @PostMapping("/read-all")
    public String markAllRead(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Object idsRaw = payload.get("noticeIds");
        if (!(idsRaw instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("noticeIds 不能为空");
        }
        return markRead(payload, request);
    }

    private void appendUpcomingExamNotices(List<Map<String, Object>> notices, Student student) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime before24h = now.plusHours(24);
        List<Exam> exams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getClassId, student.getClassId())
                        .eq(Exam::getStatus, "NOT_STARTED")
                        .ge(Exam::getStartTime, now)
                        .le(Exam::getStartTime, before24h)
                        .orderByAsc(Exam::getStartTime)
        );
        for (Exam exam : exams) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "EXAM_REMINDER");
            row.put("title", "考试即将开始");
            row.put("content", "《" + exam.getTitle() + "》将于 " + exam.getStartTime() + " 开始，请提前准备。");
            row.put("time", exam.getStartTime());
            row.put("noticeId", "EXAM_REMINDER:" + exam.getId());
            row.put("examId", exam.getId());
            notices.add(row);
        }
    }

    private void appendGradedNotices(List<Map<String, Object>> notices, Long studentId) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, studentId)
                        .eq(ExamRecord::getStatus, "GRADED")
                        .orderByDesc(ExamRecord::getSubmitTime)
                        .last("limit 10")
        );
        if (records.isEmpty()) {
            return;
        }
        Set<Long> examIds = records.stream().map(ExamRecord::getExamId).collect(Collectors.toSet());
        Map<Long, String> examTitleMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, Exam::getTitle, (a, b) -> a));
        for (ExamRecord record : records) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "GRADED");
            row.put("title", "成绩已发布");
            row.put("content", "《" + examTitleMap.getOrDefault(record.getExamId(), "未知考试") + "》成绩已发布，当前分数：" + record.getTotalScore());
            row.put("time", record.getSubmitTime());
            row.put("noticeId", "GRADED:" + record.getId());
            row.put("recordId", record.getId());
            notices.add(row);
        }
    }

    private void appendMakeupReviewNotices(List<Map<String, Object>> notices, Long studentId) {
        List<StudentMakeupRequest> makeups = makeupRequestMapper.selectList(
                new LambdaQueryWrapper<StudentMakeupRequest>()
                        .eq(StudentMakeupRequest::getStudentId, studentId)
                        .in(StudentMakeupRequest::getStatus, List.of("APPROVED", "REJECTED"))
                        .orderByDesc(StudentMakeupRequest::getReviewedAt)
                        .last("limit 10")
        );
        if (makeups.isEmpty()) {
            return;
        }
        Set<Long> examIds = makeups.stream().map(StudentMakeupRequest::getExamId).collect(Collectors.toSet());
        Map<Long, String> examTitleMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, Exam::getTitle, (a, b) -> a));

        for (StudentMakeupRequest makeup : makeups) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "MAKEUP_REVIEW");
            row.put("title", "补考申请结果");
            row.put("content", "《" + examTitleMap.getOrDefault(makeup.getExamId(), "未知考试") + "》补考申请已"
                    + ("APPROVED".equals(makeup.getStatus()) ? "通过" : "驳回")
                    + (makeup.getTeacherComment() == null || makeup.getTeacherComment().isBlank() ? "" : ("，意见：" + makeup.getTeacherComment())));
            row.put("time", makeup.getReviewedAt() == null ? makeup.getRequestedAt() : makeup.getReviewedAt());
            row.put("noticeId", "MAKEUP_REVIEW:" + makeup.getId());
            row.put("status", makeup.getStatus());
            row.put("makeupId", makeup.getId());
            notices.add(row);
        }
    }

    private Set<String> loadReadSet(Long studentId, Set<String> noticeIds) {
        if (noticeIds.isEmpty()) {
            return Set.of();
        }
        return noticeReadMapper.selectList(
                        new LambdaQueryWrapper<StudentNoticeRead>()
                                .eq(StudentNoticeRead::getStudentId, studentId)
                                .in(StudentNoticeRead::getNoticeId, noticeIds)
                ).stream()
                .map(StudentNoticeRead::getNoticeId)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> paginate(List<Map<String, Object>> rows, Integer page, Integer size) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.max(1, Math.min(size == null ? 10 : size, 100));
        int total = rows.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }
}
