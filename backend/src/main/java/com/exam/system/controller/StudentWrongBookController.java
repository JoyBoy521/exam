package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Question;
import com.exam.system.entity.Exam;
import com.exam.system.entity.PaperQuestion;
import com.exam.system.entity.StudentWrongBook;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.PaperQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.mapper.StudentWrongBookMapper;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/wrong-book")
public class StudentWrongBookController {

    private final QuestionMapper questionMapper;
    private final StudentWrongBookMapper studentWrongBookMapper;
    private final ExamMapper examMapper;
    private final PaperQuestionMapper paperQuestionMapper;

    public StudentWrongBookController(QuestionMapper questionMapper,
                                      StudentWrongBookMapper studentWrongBookMapper,
                                      ExamMapper examMapper,
                                      PaperQuestionMapper paperQuestionMapper) {
        this.questionMapper = questionMapper;
        this.studentWrongBookMapper = studentWrongBookMapper;
        this.examMapper = examMapper;
        this.paperQuestionMapper = paperQuestionMapper;
    }

    @PostMapping("/add")
    public String addToWrongBook(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Long questionId = Long.valueOf(payload.get("questionId").toString());
        String errorType = String.valueOf(payload.getOrDefault("errorType", "未分类"));
        String notes = String.valueOf(payload.getOrDefault("notes", ""));

        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("题目不存在");
        }

        LambdaQueryWrapper<StudentWrongBook> existsQuery = new LambdaQueryWrapper<StudentWrongBook>()
                .eq(StudentWrongBook::getStudentId, studentId)
                .eq(StudentWrongBook::getQuestionId, questionId);
        StudentWrongBook existed = studentWrongBookMapper.selectOne(existsQuery);
        if (existed != null) {
            existed.setErrorType(errorType);
            existed.setNotes(notes);
            studentWrongBookMapper.updateById(existed);
            return "已更新错题笔记";
        }

        StudentWrongBook wb = new StudentWrongBook();
        wb.setStudentId(studentId);
        wb.setQuestionId(questionId);
        wb.setErrorType(errorType);
        wb.setNotes(notes);
        wb.setPracticeCount(0);
        wb.setCorrectCount(0);
        wb.setMasteryLevel("UNPRACTICED");
        wb.setCreateTime(LocalDateTime.now());
        studentWrongBookMapper.insert(wb);
        return "已成功加入错题本";
    }

    @GetMapping("/list")
    public Map<String, Object> getMyWrongBook(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) Long courseId,
                                              @RequestParam(required = false) String masteryLevel,
                                              @RequestParam(required = false) String keyword,
                                              HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        List<StudentWrongBook> list = studentWrongBookMapper.selectList(
                new LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .orderByDesc(StudentWrongBook::getCreateTime)
        );

        if (list.isEmpty()) {
            return paginate(List.of(), page, size);
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
                list.stream().map(StudentWrongBook::getQuestionId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        List<Map<String, Object>> allRows = new ArrayList<>();
        for (StudentWrongBook wb : list) {
            Question q = questionMap.get(wb.getQuestionId());
            if (q == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", wb.getId());
            m.put("questionId", q.getId());
            m.put("stem", q.getStem());
            m.put("type", q.getType());
            m.put("options", q.getOptions());
            m.put("correctAnswer", q.getAnswer());
            m.put("analysis", q.getAnalysis());
            m.put("errorType", wb.getErrorType());
            m.put("notes", wb.getNotes());
            m.put("practiceCount", safeInt(wb.getPracticeCount()));
            m.put("correctCount", safeInt(wb.getCorrectCount()));
            m.put("masteryLevel", wb.getMasteryLevel());
            m.put("accuracy", computeAccuracy(wb));
            m.put("lastPracticeTime", wb.getLastPracticeTime());
            m.put("createTime", wb.getCreateTime());
            allRows.add(m);
        }

        Set<Long> allowedQuestionIds = resolveCourseQuestionIds(courseId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : allRows) {
            if (allowedQuestionIds != null && !allowedQuestionIds.contains(Long.valueOf(String.valueOf(row.get("questionId"))))) {
                continue;
            }
            if (!hitFilter(row, masteryLevel, keyword)) {
                continue;
            }
            rows.add(row);
        }
        Map<String, Object> paged = paginate(rows, page, size);
        paged.put("summary", summaryOf(allRows));
        return paged;
    }

    @GetMapping("/practice")
    public List<Map<String, Object>> getPracticeQuestions(@RequestParam(defaultValue = "20") Integer limit,
                                                          @RequestParam(required = false) Long courseId,
                                                          HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));

        List<StudentWrongBook> list = studentWrongBookMapper.selectList(
                new LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .orderByDesc(StudentWrongBook::getCreateTime)
        );
        Set<Long> allowedQuestionIds = resolveCourseQuestionIds(courseId);
        if (allowedQuestionIds != null && !allowedQuestionIds.isEmpty()) {
            list = list.stream().filter(x -> allowedQuestionIds.contains(x.getQuestionId())).toList();
        }
        if (list.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> questionIds = new LinkedHashSet<>();
        for (StudentWrongBook item : list) {
            questionIds.add(item.getQuestionId());
            if (questionIds.size() >= safeLimit) {
                break;
            }
        }
        Map<Long, Question> qMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, StudentWrongBook> wbMap = list.stream()
                .collect(Collectors.toMap(StudentWrongBook::getQuestionId, x -> x, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        int index = 1;
        for (Long qid : questionIds) {
            Question q = qMap.get(qid);
            if (q == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", q.getId());
            row.put("seq", index++);
            row.put("stem", q.getStem());
            row.put("type", q.getType());
            row.put("options", q.getOptions());
            row.put("analysis", q.getAnalysis());
            StudentWrongBook wb = wbMap.get(q.getId());
            if (wb != null) {
                row.put("masteryLevel", wb.getMasteryLevel());
                row.put("accuracy", computeAccuracy(wb));
                row.put("practiceCount", safeInt(wb.getPracticeCount()));
            } else {
                row.put("masteryLevel", "UNPRACTICED");
                row.put("accuracy", 0.0);
                row.put("practiceCount", 0);
            }
            result.add(row);
        }
        return result;
    }

    @GetMapping("/daily-plan")
    public Map<String, Object> dailyPlan(@RequestParam(defaultValue = "10") Integer targetCount,
                                         HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        int safeTarget = Math.max(5, Math.min(targetCount == null ? 10 : targetCount, 30));

        List<StudentWrongBook> all = studentWrongBookMapper.selectList(
                new LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .orderByDesc(StudentWrongBook::getCreateTime)
        );
        if (all.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("targetCount", safeTarget);
            empty.put("dueCount", 0);
            empty.put("highPriorityCount", 0);
            empty.put("recommended", List.of());
            return empty;
        }

        LocalDateTime reviewDeadline = LocalDateTime.now().minusDays(3);
        List<StudentWrongBook> due = all.stream().filter(wb ->
                !"MASTERED".equals(wb.getMasteryLevel())
                        || wb.getLastPracticeTime() == null
                        || wb.getLastPracticeTime().isBefore(reviewDeadline)
        ).toList();
        long highPriorityCount = due.stream().filter(wb -> "NEEDS_REVIEW".equals(wb.getMasteryLevel())).count();

        List<StudentWrongBook> sorted = new ArrayList<>(due);
        sorted.sort((a, b) -> {
            int pa = priority(a.getMasteryLevel());
            int pb = priority(b.getMasteryLevel());
            if (pa != pb) return Integer.compare(pb, pa);
            int ap = safeInt(a.getPracticeCount());
            int bp = safeInt(b.getPracticeCount());
            if (ap != bp) return Integer.compare(ap, bp);
            LocalDateTime at = a.getLastPracticeTime() == null ? LocalDateTime.MIN : a.getLastPracticeTime();
            LocalDateTime bt = b.getLastPracticeTime() == null ? LocalDateTime.MIN : b.getLastPracticeTime();
            return at.compareTo(bt);
        });

        List<Long> questionIds = sorted.stream().limit(safeTarget).map(StudentWrongBook::getQuestionId).toList();
        Map<Long, Question> qMap = questionIds.isEmpty()
                ? Map.of()
                : questionMapper.selectBatchIds(questionIds).stream().collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        List<Map<String, Object>> recommended = new ArrayList<>();
        for (StudentWrongBook wb : sorted.stream().limit(safeTarget).toList()) {
            Question q = qMap.get(wb.getQuestionId());
            if (q == null) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("questionId", q.getId());
            row.put("stem", q.getStem());
            row.put("type", q.getType());
            row.put("masteryLevel", wb.getMasteryLevel());
            row.put("accuracy", computeAccuracy(wb));
            row.put("practiceCount", safeInt(wb.getPracticeCount()));
            row.put("lastPracticeTime", wb.getLastPracticeTime());
            recommended.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetCount", safeTarget);
        result.put("dueCount", due.size());
        result.put("highPriorityCount", highPriorityCount);
        result.put("recommended", recommended);
        return result;
    }

    @GetMapping("/weekly-report")
    public Map<String, Object> weeklyReport(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        List<StudentWrongBook> all = studentWrongBookMapper.selectList(
                new LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .orderByDesc(StudentWrongBook::getCreateTime)
        );
        int totalWrongCount = all.size();
        int practicedCount = (int) all.stream().filter(x -> safeInt(x.getPracticeCount()) > 0).count();
        int masteredCount = (int) all.stream().filter(x -> "MASTERED".equals(x.getMasteryLevel())).count();
        int needReviewCount = (int) all.stream().filter(x -> "NEEDS_REVIEW".equals(x.getMasteryLevel())).count();
        int practiceTimes = all.stream().mapToInt(x -> safeInt(x.getPracticeCount())).sum();
        double avgAccuracy = totalWrongCount == 0 ? 0 : Math.round(all.stream().mapToDouble(this::computeAccuracy).average().orElse(0.0) * 100.0) / 100.0;

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        Map<LocalDate, Integer> trendMap = new HashMap<>();
        for (StudentWrongBook item : all) {
            if (item.getLastPracticeTime() == null) {
                continue;
            }
            LocalDate day = item.getLastPracticeTime().toLocalDate();
            if (day.isBefore(start) || day.isAfter(today)) {
                continue;
            }
            trendMap.merge(day, 1, Integer::sum);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            Map<String, Object> row = new HashMap<>();
            row.put("date", day.toString());
            row.put("count", trendMap.getOrDefault(day, 0));
            trend.add(row);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalWrongCount", totalWrongCount);
        report.put("practicedCount", practicedCount);
        report.put("masteredCount", masteredCount);
        report.put("needReviewCount", needReviewCount);
        report.put("practiceTimes", practiceTimes);
        report.put("avgAccuracy", avgAccuracy);
        report.put("weeklyTrend", trend);
        return report;
    }

    @PostMapping("/practice/submit")
    public Map<String, Object> submitPractice(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Object answersRaw = payload.get("answers");
        if (!(answersRaw instanceof Map<?, ?> answersMap)) {
            throw new IllegalArgumentException("answers 参数错误");
        }

        List<Map<String, Object>> details = new ArrayList<>();
        int objectiveCount = 0;
        int objectiveCorrect = 0;
        int shortAnswerCount = 0;
        Map<Long, String> userAnswers = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : answersMap.entrySet()) {
            String qidText = String.valueOf(entry.getKey());
            if (qidText.isBlank()) continue;
            userAnswers.put(Long.valueOf(qidText), String.valueOf(entry.getValue() == null ? "" : entry.getValue()));
        }
        if (userAnswers.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("objectiveCount", 0);
            result.put("objectiveCorrect", 0);
            result.put("objectiveAccuracy", 0);
            result.put("shortAnswerCount", 0);
            result.put("details", List.of());
            return result;
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(userAnswers.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        Map<Long, StudentWrongBook> wbMap = studentWrongBookMapper.selectList(
                        new LambdaQueryWrapper<StudentWrongBook>()
                                .eq(StudentWrongBook::getStudentId, studentId)
                                .in(StudentWrongBook::getQuestionId, userAnswers.keySet())
                ).stream()
                .collect(Collectors.toMap(StudentWrongBook::getQuestionId, x -> x, (a, b) -> a));

        for (Map.Entry<Long, String> entry : userAnswers.entrySet()) {
            Long questionId = entry.getKey();
            Question q = questionMap.get(questionId);
            if (q == null) continue;

            String userAnswer = normalizeAnswerByType(q.getType(), entry.getValue());
            String correctAnswer = normalizeAnswerByType(q.getType(), q.getAnswer());
            Boolean isCorrect = null;
            if (!"SHORT_ANSWER".equals(q.getType())) {
                objectiveCount++;
                isCorrect = Objects.equals(userAnswer, correctAnswer);
                if (Boolean.TRUE.equals(isCorrect)) {
                    objectiveCorrect++;
                }
                updateWrongBookMastery(wbMap.get(q.getId()), isCorrect);
            } else {
                shortAnswerCount++;
                updateWrongBookSubjectivePractice(wbMap.get(q.getId()));
            }

            Map<String, Object> row = new HashMap<>();
            row.put("questionId", q.getId());
            row.put("type", q.getType());
            row.put("stem", q.getStem());
            row.put("userAnswer", userAnswer);
            row.put("correctAnswer", q.getAnswer());
            row.put("analysis", q.getAnalysis());
            row.put("isCorrect", isCorrect);
            StudentWrongBook wb = wbMap.get(q.getId());
            if (wb != null) {
                row.put("masteryLevel", wb.getMasteryLevel());
                row.put("accuracy", computeAccuracy(wb));
                row.put("practiceCount", safeInt(wb.getPracticeCount()));
                row.put("correctCount", safeInt(wb.getCorrectCount()));
            }
            details.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("objectiveCount", objectiveCount);
        result.put("objectiveCorrect", objectiveCorrect);
        result.put("objectiveAccuracy", objectiveCount == 0 ? 0 : Math.round(objectiveCorrect * 10000.0 / objectiveCount) / 100.0);
        result.put("shortAnswerCount", shortAnswerCount);
        result.put("details", details);
        return result;
    }

    @DeleteMapping("/{id}")
    public String remove(@PathVariable Long id, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        StudentWrongBook record = studentWrongBookMapper.selectById(id);
        if (record == null || !Objects.equals(record.getStudentId(), studentId)) {
            throw new IllegalArgumentException("记录不存在或无权限删除");
        }
        studentWrongBookMapper.deleteById(id);
        return "已从错题本移除";
    }

    private String normalizeAnswerByType(String type, String answer) {
        if (answer == null) {
            return "";
        }
        String trimmed = answer.trim();
        if ("MULTIPLE_CHOICE".equals(type)) {
            if (trimmed.isBlank()) {
                return "";
            }
            return Arrays.stream(trimmed.toUpperCase().replaceAll("\\s+", "").split(","))
                    .filter(x -> !x.isBlank())
                    .map(x -> x.substring(0, 1))
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        return trimmed;
    }

    private void updateWrongBookMastery(StudentWrongBook wb, Boolean isCorrect) {
        if (wb == null) return;
        int practice = safeInt(wb.getPracticeCount()) + 1;
        int correct = safeInt(wb.getCorrectCount()) + (Boolean.TRUE.equals(isCorrect) ? 1 : 0);
        wb.setPracticeCount(practice);
        wb.setCorrectCount(correct);
        wb.setLastPracticeTime(LocalDateTime.now());
        wb.setMasteryLevel(resolveMasteryLevel(practice, correct));
        studentWrongBookMapper.updateById(wb);
    }

    private void updateWrongBookSubjectivePractice(StudentWrongBook wb) {
        if (wb == null) return;
        wb.setPracticeCount(safeInt(wb.getPracticeCount()) + 1);
        wb.setMasteryLevel("SUBJECTIVE");
        wb.setLastPracticeTime(LocalDateTime.now());
        studentWrongBookMapper.updateById(wb);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double computeAccuracy(StudentWrongBook wb) {
        int practice = safeInt(wb.getPracticeCount());
        int correct = safeInt(wb.getCorrectCount());
        if (practice == 0) {
            return 0.0;
        }
        return Math.round(correct * 10000.0 / practice) / 100.0;
    }

    private String resolveMasteryLevel(int practiceCount, int correctCount) {
        if (practiceCount <= 0) {
            return "UNPRACTICED";
        }
        double acc = (double) correctCount / practiceCount;
        if (practiceCount >= 5 && acc >= 0.85) {
            return "MASTERED";
        }
        if (practiceCount >= 3 && acc >= 0.6) {
            return "IMPROVING";
        }
        return "NEEDS_REVIEW";
    }

    private int priority(String masteryLevel) {
        if ("NEEDS_REVIEW".equals(masteryLevel)) return 4;
        if ("UNPRACTICED".equals(masteryLevel) || masteryLevel == null || masteryLevel.isBlank()) return 3;
        if ("IMPROVING".equals(masteryLevel)) return 2;
        if ("SUBJECTIVE".equals(masteryLevel)) return 1;
        return 0;
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

    private boolean hitFilter(Map<String, Object> row, String masteryLevel, String keyword) {
        if (masteryLevel != null && !masteryLevel.isBlank()) {
            if (!masteryLevel.trim().equalsIgnoreCase(String.valueOf(row.getOrDefault("masteryLevel", "")))) {
                return false;
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            return String.valueOf(row.getOrDefault("stem", "")).toLowerCase().contains(kw)
                    || String.valueOf(row.getOrDefault("notes", "")).toLowerCase().contains(kw)
                    || String.valueOf(row.getOrDefault("errorType", "")).toLowerCase().contains(kw);
        }
        return true;
    }

    private Set<Long> resolveCourseQuestionIds(Long courseId) {
        if (courseId == null) {
            return null;
        }
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getCourseId, courseId));
        if (exams.isEmpty()) {
            return Set.of();
        }
        Set<Long> paperIds = new LinkedHashSet<>();
        for (Exam exam : exams) {
            if (exam.getPaperIds() == null || exam.getPaperIds().isBlank()) {
                continue;
            }
            for (String part : exam.getPaperIds().split(",")) {
                String t = part.trim();
                if (t.isBlank()) {
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
        return paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>().in(PaperQuestion::getPaperId, paperIds))
                .stream().map(PaperQuestion::getQuestionId).collect(Collectors.toSet());
    }

    private Map<String, Object> summaryOf(List<Map<String, Object>> rows) {
        int totalCount = rows.size();
        int masteredCount = 0;
        int needReviewCount = 0;
        double sumAccuracy = 0.0;
        for (Map<String, Object> row : rows) {
            String level = String.valueOf(row.getOrDefault("masteryLevel", ""));
            if ("MASTERED".equals(level)) {
                masteredCount++;
            }
            if ("NEEDS_REVIEW".equals(level)) {
                needReviewCount++;
            }
            sumAccuracy += Double.parseDouble(String.valueOf(row.getOrDefault("accuracy", 0.0)));
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("masteredCount", masteredCount);
        summary.put("needReviewCount", needReviewCount);
        summary.put("avgAccuracy", totalCount == 0 ? 0 : Math.round(sumAccuracy * 100.0 / totalCount) / 100.0);
        return summary;
    }
}
