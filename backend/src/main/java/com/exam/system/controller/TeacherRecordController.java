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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;
    private final ExamMapper examMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final TeacherExamService teacherExamService;
    private final AuditLogService auditLogService;

    public TeacherRecordController(ExamRecordMapper examRecordMapper,
                                   ExamRecordAnswerMapper examRecordAnswerMapper,
                                   QuestionMapper questionMapper,
                                   StudentMapper studentMapper,
                                   SysUserMapper sysUserMapper,
                                   AuditLogMapper auditLogMapper,
                                   ExamMapper examMapper,
                                   PaperQuestionMapper paperQuestionMapper,
                                   TeacherExamService teacherExamService,
                                   AuditLogService auditLogService) {
        this.examRecordMapper = examRecordMapper;
        this.examRecordAnswerMapper = examRecordAnswerMapper;
        this.questionMapper = questionMapper;
        this.studentMapper = studentMapper;
        this.sysUserMapper = sysUserMapper;
        this.auditLogMapper = auditLogMapper;
        this.examMapper = examMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.teacherExamService = teacherExamService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/exam/{examId}")
    public Map<String, Object> getRecordsByExam(@PathVariable Long examId,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "20") Integer size) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .orderByDesc(ExamRecord::getSubmitTime)
        );

        Set<Long> studentIds = records.stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> rows = new ArrayList<>();
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
            rows.add(map);
        }
        return paginate(rows, page, size);
    }

    @PostMapping("/{recordId}/grade")
    public void gradeRecord(@PathVariable Long recordId,
                            @RequestBody Map<String, Object> payload,
                            HttpServletRequest request) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        BigDecimal subjectiveScore = computeAndPersistSubjectiveScore(record, payload);
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
                "studentId=" + record.getUserId()
                        + ",subjectiveScore=" + subjectiveScore
                        + ",items=" + buildSubjectiveItemsBrief(payload)
        );
    }

    @PostMapping("/batch-action")
    public Map<String, Object> batchAction(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Object idsRaw = payload.get("recordIds");
        if (!(idsRaw instanceof List<?> rawList) || rawList.isEmpty()) {
            throw new IllegalArgumentException("recordIds 不能为空");
        }
        String action = String.valueOf(payload.getOrDefault("action", "")).trim();
        if (!"SETTLE_NO_SUBJECTIVE".equals(action) && !"REOPEN_MARKING".equals(action)) {
            throw new IllegalArgumentException("action 不支持");
        }

        List<Long> recordIds = rawList.stream()
                .map(String::valueOf)
                .map(Long::valueOf)
                .distinct()
                .toList();

        int updated = 0;
        List<Long> skipped = new ArrayList<>();
        for (Long recordId : recordIds) {
            ExamRecord record = examRecordMapper.selectById(recordId);
            if (record == null) {
                skipped.add(recordId);
                continue;
            }
            if ("REOPEN_MARKING".equals(action)) {
                record.setStatus("MARKING");
                examRecordMapper.updateById(record);
                updated++;
                continue;
            }

            if (hasSubjectiveQuestion(recordId)) {
                skipped.add(recordId);
                continue;
            }
            record.setSubjectiveScore(BigDecimal.ZERO);
            BigDecimal objectiveScore = record.getObjectiveScore() == null ? BigDecimal.ZERO : record.getObjectiveScore();
            record.setTotalScore(objectiveScore);
            record.setStatus("GRADED");
            examRecordMapper.updateById(record);
            updated++;
        }

        auditLogService.record(
                CurrentUser.userId(request),
                "BATCH_RECORD_ACTION",
                "EXAM_RECORD",
                null,
                "action=" + action + ",updated=" + updated + ",requested=" + recordIds.size() + ",recordIds=" + recordIds
        );

        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("skipped", skipped);
        return result;
    }

    @GetMapping("/exam/{examId}/grading-history")
    public List<Map<String, Object>> gradingHistory(@PathVariable Long examId,
                                                    @RequestParam(required = false) String action,
                                                    @RequestParam(required = false) Long operatorId,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime,
                                                    @RequestParam(required = false) String keyword) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId)
        );
        if (records.isEmpty()) {
            return List.of();
        }

        Set<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toSet());
        Map<Long, ExamRecord> recordMap = records.stream().collect(Collectors.toMap(ExamRecord::getId, r -> r));
        Set<Long> studentIds = records.stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));

        LambdaQueryWrapper<AuditLog> logQuery = new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getTargetType, "EXAM_RECORD")
                .and(w -> w.eq(AuditLog::getAction, "GRADE_RECORD")
                        .or().eq(AuditLog::getAction, "BATCH_RECORD_ACTION"));
        if (action != null && !action.isBlank()) {
            logQuery.eq(AuditLog::getAction, action.trim());
        }
        if (operatorId != null) {
            logQuery.eq(AuditLog::getUserId, operatorId);
        }
        LocalDateTime start = parseDateTime(startTime, true);
        LocalDateTime end = parseDateTime(endTime, false);
        if (start != null) {
            logQuery.ge(AuditLog::getCreatedAt, start);
        }
        if (end != null) {
            logQuery.le(AuditLog::getCreatedAt, end);
        }
        logQuery.orderByDesc(AuditLog::getCreatedAt);
        List<AuditLog> logs = auditLogMapper.selectList(logQuery);
        if (logs.isEmpty()) {
            return List.of();
        }

        Set<Long> operatorIds = logs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> operatorMap = operatorIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditLog logRow : logs) {
            Long targetId = logRow.getTargetId();
            if (targetId != null && !recordIds.contains(targetId)) {
                continue;
            }
            if (targetId == null && !involvesExam(logRow.getDetail(), recordIds)) {
                continue;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("id", logRow.getId());
            row.put("action", logRow.getAction());
            row.put("actionText", "GRADE_RECORD".equals(logRow.getAction()) ? "批阅提交" : "批量操作");
            row.put("recordId", targetId);
            if (targetId != null) {
                ExamRecord rec = recordMap.get(targetId);
                if (rec != null) {
                    Student st = studentMap.get(rec.getUserId());
                    row.put("studentNo", st == null ? null : st.getStudentNo());
                    row.put("studentName", st == null ? null : st.getName());
                }
            }
            SysUser operator = operatorMap.get(logRow.getUserId());
            row.put("operatorId", logRow.getUserId());
            row.put("operator", operator == null ? ("user#" + logRow.getUserId()) : operator.getDisplayName());
            row.put("detail", logRow.getDetail());
            row.put("createdAt", logRow.getCreatedAt());
            if (!hitHistoryKeyword(row, keyword)) {
                continue;
            }
            result.add(row);
        }
        return result;
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
                map.put("teacherComment", ans.getTeacherComment());
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

    private boolean hasSubjectiveQuestion(Long recordId) {
        List<ExamRecordAnswer> answers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, recordId)
        );
        if (answers.isEmpty()) {
            return false;
        }
        Set<Long> qids = answers.stream().map(ExamRecordAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        return answers.stream()
                .map(ExamRecordAnswer::getQuestionId)
                .map(qMap::get)
                .anyMatch(q -> q != null && "SHORT_ANSWER".equals(q.getType()));
    }

    private BigDecimal computeAndPersistSubjectiveScore(ExamRecord record, Map<String, Object> payload) {
        List<ExamRecordAnswer> recordAnswers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, record.getId())
        );
        if (recordAnswers.isEmpty()) {
            return parseSubjectiveScore(payload);
        }

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
                recordAnswers.stream().map(ExamRecordAnswer::getQuestionId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        Exam exam = examMapper.selectById(record.getExamId());
        Long paperId = pickAssignedPaperId(exam != null ? exam.getPaperIds() : null, record.getUserId());
        Map<Long, BigDecimal> scoreMap = paperQuestionMapper.selectList(
                        new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId)
                ).stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore, (a, b) -> a));

        Object subjectiveItemsRaw = payload.get("subjectiveItems");
        if (!(subjectiveItemsRaw instanceof List<?> subjectiveItems) || subjectiveItems.isEmpty()) {
            return parseSubjectiveScore(payload);
        }

        Map<Long, BigDecimal> givenScoreMap = new HashMap<>();
        Map<Long, String> commentMap = new HashMap<>();
        for (Object row : subjectiveItems) {
            if (!(row instanceof Map<?, ?> item)) {
                continue;
            }
            Long answerId = Long.valueOf(String.valueOf(item.get("answerId")));
            Object givenScoreRaw = item.get("givenScore");
            BigDecimal givenScore = new BigDecimal(String.valueOf(givenScoreRaw == null ? "0" : givenScoreRaw));
            if (givenScore.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("题目分数不能小于0");
            }
            givenScoreMap.put(answerId, givenScore);
            Object commentRaw = item.get("teacherComment");
            String comment = commentRaw == null ? "" : String.valueOf(commentRaw).trim();
            if (comment.length() > 500) {
                comment = comment.substring(0, 500);
            }
            commentMap.put(answerId, comment);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (ExamRecordAnswer answer : recordAnswers) {
            Question q = questionMap.get(answer.getQuestionId());
            if (q == null || !"SHORT_ANSWER".equals(q.getType())) {
                continue;
            }
            BigDecimal max = Optional.ofNullable(scoreMap.get(q.getId())).orElse(BigDecimal.TEN);
            BigDecimal given = givenScoreMap.get(answer.getId());
            if (given == null) {
                given = BigDecimal.valueOf(answer.getScore() == null ? 0 : answer.getScore());
            }
            if (given.compareTo(max) > 0) {
                throw new IllegalArgumentException("题目分数不能超过满分");
            }
            answer.setScore(given.doubleValue());
            answer.setTeacherComment(commentMap.getOrDefault(answer.getId(), ""));
            examRecordAnswerMapper.updateById(answer);
            total = total.add(given);
        }
        return total;
    }

    private BigDecimal parseSubjectiveScore(Map<String, Object> payload) {
        Object value = payload.get("subjectiveScore");
        if (value == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal score = new BigDecimal(String.valueOf(value));
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("主观题总分不能小于0");
        }
        return score;
    }

    private String buildSubjectiveItemsBrief(Map<String, Object> payload) {
        Object subjectiveItemsRaw = payload.get("subjectiveItems");
        if (!(subjectiveItemsRaw instanceof List<?> subjectiveItems) || subjectiveItems.isEmpty()) {
            return "[]";
        }
        return subjectiveItems.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> {
                    Object answerId = item.get("answerId");
                    Object score = item.get("givenScore");
                    Object comment = item.get("teacherComment");
                    String c = comment == null ? "" : String.valueOf(comment).replace(",", "，");
                    if (c.length() > 40) {
                        c = c.substring(0, 40) + "...";
                    }
                    return "{a=" + answerId + ",s=" + score + ",c=" + c + "}";
                })
                .collect(Collectors.joining(";"));
    }

    private boolean involvesExam(String detail, Set<Long> recordIds) {
        if (detail == null || detail.isBlank()) {
            return false;
        }
        int idx = detail.indexOf("recordIds=");
        if (idx < 0) {
            return false;
        }
        String list = detail.substring(idx + "recordIds=".length());
        for (Long id : recordIds) {
            if (list.contains(String.valueOf(id))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> rows, Integer page, Integer size) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.max(1, Math.min(size == null ? 20 : size, 200));
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

    private LocalDateTime parseDateTime(String text, boolean startOfDayFallback) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignore) {
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignore) {
        }
        try {
            LocalDate d = LocalDate.parse(value);
            return startOfDayFallback ? d.atStartOfDay() : d.atTime(23, 59, 59);
        } catch (Exception ignore) {
        }
        return null;
    }

    private boolean hitHistoryKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        return String.valueOf(row.getOrDefault("operator", "")).toLowerCase().contains(kw)
                || String.valueOf(row.getOrDefault("studentNo", "")).toLowerCase().contains(kw)
                || String.valueOf(row.getOrDefault("studentName", "")).toLowerCase().contains(kw)
                || String.valueOf(row.getOrDefault("detail", "")).toLowerCase().contains(kw)
                || String.valueOf(row.getOrDefault("recordId", "")).toLowerCase().contains(kw);
    }
}
