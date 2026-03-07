package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Question;
import com.exam.system.entity.StudentWrongBook;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.mapper.StudentWrongBookMapper;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/wrong-book")
public class StudentWrongBookController {

    private final QuestionMapper questionMapper;
    private final StudentWrongBookMapper studentWrongBookMapper;

    public StudentWrongBookController(QuestionMapper questionMapper, StudentWrongBookMapper studentWrongBookMapper) {
        this.questionMapper = questionMapper;
        this.studentWrongBookMapper = studentWrongBookMapper;
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

        List<Map<String, Object>> rows = new ArrayList<>();
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
            if (!hitFilter(m, masteryLevel, keyword)) {
                continue;
            }
            rows.add(m);
        }
        return paginate(rows, page, size);
    }

    @GetMapping("/practice")
    public List<Map<String, Object>> getPracticeQuestions(@RequestParam(defaultValue = "20") Integer limit,
                                                          HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));

        List<StudentWrongBook> list = studentWrongBookMapper.selectList(
                new LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .orderByDesc(StudentWrongBook::getCreateTime)
        );
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
}
