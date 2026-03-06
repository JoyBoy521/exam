package com.exam.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.*;
import com.exam.system.entity.*;
import com.exam.system.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherExamService {

    private final QuestionMapper questionMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamRecordAnswerMapper examRecordAnswerMapper;
    private final StudentMapper studentMapper;
    private final ParallelGroupMapper parallelGroupMapper;
    private final ParallelAssignmentMapper parallelAssignmentMapper;
    private final StudentWrongBookMapper studentWrongBookMapper;
    private final ExamAnswerDraftMapper examAnswerDraftMapper;

    public TeacherExamService(QuestionMapper questionMapper,
                              PaperMapper paperMapper,
                              PaperQuestionMapper paperQuestionMapper,
                              ExamMapper examMapper,
                              ExamRecordMapper examRecordMapper,
                              ExamRecordAnswerMapper examRecordAnswerMapper,
                              StudentMapper studentMapper,
                              ParallelGroupMapper parallelGroupMapper,
                              ParallelAssignmentMapper parallelAssignmentMapper,
                              StudentWrongBookMapper studentWrongBookMapper,
                              ExamAnswerDraftMapper examAnswerDraftMapper) {
        this.questionMapper = questionMapper;
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.examRecordAnswerMapper = examRecordAnswerMapper;
        this.studentMapper = studentMapper;
        this.parallelGroupMapper = parallelGroupMapper;
        this.parallelAssignmentMapper = parallelAssignmentMapper;
        this.studentWrongBookMapper = studentWrongBookMapper;
        this.examAnswerDraftMapper = examAnswerDraftMapper;
    }

    public Exam createExam(Map<String, Object> payload) {
        LocalDateTime start = LocalDateTime.parse((String) payload.get("startTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime end = LocalDateTime.parse((String) payload.get("endTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Exam exam = new Exam();
        exam.setTitle((String) payload.get("title"));
        exam.setClassId(Long.valueOf(payload.get("classId").toString()));

        List<?> pIdsRaw = (List<?>) payload.get("paperIds");
        if (pIdsRaw == null || pIdsRaw.isEmpty()) {
            throw new IllegalArgumentException("至少选择一套试卷");
        }
        String paperIdsStr = pIdsRaw.stream().map(String::valueOf).collect(Collectors.joining(","));
        exam.setPaperIds(paperIdsStr);

        exam.setStartTime(start);
        exam.setEndTime(end);
        exam.setStatus(calcStatus(start, end));
        exam.setCreateTime(LocalDateTime.now());
        examMapper.insert(exam);
        return exam;
    }

    public List<Exam> listExams() {
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreateTime));
        exams.forEach(this::syncExamStatusIfNeeded);
        return exams;
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        if ("FINISHED".equals(exam.getStatus())) {
            return;
        }
        exam.setStatus("FINISHED");
        exam.setEndTime(LocalDateTime.now());
        examMapper.updateById(exam);
    }

    public List<Exam> listStudentAvailableExams(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }

        LambdaQueryWrapper<Exam> query = new LambdaQueryWrapper<>();
        if (student.getClassId() != null) {
            query.eq(Exam::getClassId, student.getClassId());
        }
        query.orderByDesc(Exam::getCreateTime);

        List<Exam> exams = examMapper.selectList(query);
        exams.forEach(this::syncExamStatusIfNeeded);
        return exams;
    }

    public StudentExamDetailResponse getStudentExamDetail(Long examId, Long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new NoSuchElementException("考试不存在");
        }
        syncExamStatusIfNeeded(exam);

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        if (exam.getClassId() != null && student.getClassId() != null && !Objects.equals(exam.getClassId(), student.getClassId())) {
            throw new IllegalArgumentException("无权限访问该考试");
        }

        Long assignedPaperId = pickAssignedPaperId(exam.getPaperIds(), studentId);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, assignedPaperId)
                        .orderByAsc(PaperQuestion::getSortOrder, PaperQuestion::getId)
        );

        List<Long> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<StudentExamDetailResponse.QuestionItem> questionItems = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            Question q = qMap.get(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            questionItems.add(new StudentExamDetailResponse.QuestionItem(q.getId(), q.getType(), q.getStem(), q.getOptions()));
        }

        return new StudentExamDetailResponse(
                exam.getId(), exam.getTitle(), exam.getStartTime(), exam.getEndTime(), exam.getStatus(), questionItems
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamRecord submitStudentExam(Long examId, Long studentId, Map<Long, String> answers) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new NoSuchElementException("考试不存在");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        if (exam.getClassId() != null && student.getClassId() != null && !Objects.equals(exam.getClassId(), student.getClassId())) {
            throw new IllegalArgumentException("无权限提交该考试");
        }
        if (exam.getEndTime() != null && LocalDateTime.now().isAfter(exam.getEndTime())) {
            throw new IllegalArgumentException("考试已结束，禁止交卷");
        }
        syncExamStatusIfNeeded(exam);

        if ("NOT_STARTED".equals(exam.getStatus())) {
            throw new IllegalArgumentException("考试未开始");
        }
        if ("FINISHED".equals(exam.getStatus())) {
            throw new IllegalArgumentException("考试已结束");
        }

        LambdaQueryWrapper<ExamRecord> existedQuery = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, studentId);
        if (examRecordMapper.selectCount(existedQuery) > 0) {
            throw new IllegalArgumentException("该考试已提交，请勿重复交卷");
        }

        Long paperId = pickAssignedPaperId(exam.getPaperIds(), studentId);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, paperId)
                        .orderByAsc(PaperQuestion::getSortOrder, PaperQuestion::getId)
        );
        Map<Long, PaperQuestion> scoreMap = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, x -> x, (a, b) -> a));

        List<Long> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream().collect(Collectors.toMap(Question::getId, q -> q));

        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setUserId(studentId);
        record.setStartTime(LocalDateTime.now());
        record.setSubmitTime(LocalDateTime.now());
        examRecordMapper.insert(record);

        BigDecimal objectiveScore = BigDecimal.ZERO;
        boolean hasSubjective = false;

        for (PaperQuestion pq : paperQuestions) {
            Long qid = pq.getQuestionId();
            Question q = qMap.get(qid);
            if (q == null) {
                continue;
            }

            String userAnswer = normalizeAnswerByType(q.getType(), Optional.ofNullable(answers.get(qid)).orElse(""));
            ExamRecordAnswer answer = new ExamRecordAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(qid);
            answer.setUserAnswer(userAnswer);

            if ("SHORT_ANSWER".equals(q.getType())) {
                hasSubjective = true;
                answer.setIsCorrect(null);
                answer.setScore(0.0);
            } else {
                boolean isCorrect = isObjectiveCorrect(q.getType(), q.getAnswer(), userAnswer);
                BigDecimal score = isCorrect ? Optional.ofNullable(scoreMap.get(qid)).map(PaperQuestion::getScore).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                answer.setIsCorrect(isCorrect ? 1 : 0);
                answer.setScore(score.doubleValue());
                objectiveScore = objectiveScore.add(score);
            }
            examRecordAnswerMapper.insert(answer);
        }

        record.setObjectiveScore(objectiveScore);
        if (hasSubjective) {
            record.setSubjectiveScore(BigDecimal.ZERO);
            record.setTotalScore(objectiveScore);
            record.setStatus("MARKING");
        } else {
            record.setSubjectiveScore(BigDecimal.ZERO);
            record.setTotalScore(objectiveScore);
            record.setStatus("GRADED");
        }
        examRecordMapper.updateById(record);
        examAnswerDraftMapper.delete(new LambdaQueryWrapper<ExamAnswerDraft>()
                .eq(ExamAnswerDraft::getExamId, examId)
                .eq(ExamAnswerDraft::getStudentId, studentId));
        if (!hasSubjective) {
            syncWrongBookForRecord(record.getId(), studentId);
        }
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncWrongBookForRecord(Long recordId, Long studentId) {
        List<ExamRecordAnswer> wrongAnswers = examRecordAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamRecordAnswer>()
                        .eq(ExamRecordAnswer::getRecordId, recordId)
                        .eq(ExamRecordAnswer::getIsCorrect, 0)
        );
        if (wrongAnswers.isEmpty()) {
            return;
        }

        for (ExamRecordAnswer wrongAnswer : wrongAnswers) {
            Long questionId = wrongAnswer.getQuestionId();
            StudentWrongBook existed = studentWrongBookMapper.selectOne(
                    new LambdaQueryWrapper<StudentWrongBook>()
                            .eq(StudentWrongBook::getStudentId, studentId)
                            .eq(StudentWrongBook::getQuestionId, questionId)
                            .last("limit 1")
            );
            if (existed != null) {
                continue;
            }
            StudentWrongBook wb = new StudentWrongBook();
            wb.setStudentId(studentId);
            wb.setQuestionId(questionId);
            wb.setErrorType("系统自动归档");
            wb.setNotes("考试错题自动加入");
            wb.setCreateTime(LocalDateTime.now());
            studentWrongBookMapper.insert(wb);
        }
    }

    public List<TeacherSubmissionViewResponse> listExamSubmissions(Long examId) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId).orderByDesc(ExamRecord::getSubmitTime)
        );
        if (records.isEmpty()) {
            return List.of();
        }

        Set<Long> studentIds = records.stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        return records.stream().map(r -> {
            Student student = studentMap.get(r.getUserId());
            String stuName = student != null ? student.getName() : ("学号:" + r.getUserId());
            return new TeacherSubmissionViewResponse(
                    r.getId(),
                    stuName,
                    r.getObjectiveScore() != null ? r.getObjectiveScore().doubleValue() : 0.0,
                    r.getSubjectiveScore() != null ? r.getSubjectiveScore().doubleValue() : 0.0,
                    r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0.0,
                    r.getStatus(),
                    r.getSubmitTime()
            );
        }).toList();
    }

    public StatisticsResponse statistics() {
        long qCount = questionMapper.selectCount(null);
        long pCount = paperMapper.selectCount(null);
        long examCount = examMapper.selectCount(null);
        long parallelGroupCount = parallelGroupMapper.selectCount(null);
        long assignmentCount = parallelAssignmentMapper.selectCount(null);

        List<Question> questions = questionMapper.selectList(null);
        Map<String, Long> typeDist = questions.stream()
                .collect(Collectors.groupingBy(Question::getType, Collectors.counting()));

        Map<String, Long> kpCount = new HashMap<>();
        for (Question q : questions) {
            if (q.getKnowledgePoints() == null) {
                continue;
            }
            for (String kp : q.getKnowledgePoints()) {
                kpCount.merge(kp, 1L, Long::sum);
            }
        }
        List<StatisticsResponse.KnowledgeCoverage> topKnowledge = kpCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> new StatisticsResponse.KnowledgeCoverage(e.getKey(), e.getValue()))
                .toList();

        Map<String, Long> paperDist = new HashMap<>();
        List<ParallelAssignment> assignments = parallelAssignmentMapper.selectList(null);
        for (ParallelAssignment a : assignments) {
            paperDist.merge(String.valueOf(a.getPaperId()), 1L, Long::sum);
        }

        return new StatisticsResponse(
                qCount,
                pCount,
                parallelGroupCount,
                assignmentCount,
                typeDist,
                topKnowledge,
                paperDist
        );
    }

    public TeacherTodoResponse teacherTodo() {
        List<Exam> exams = examMapper.selectList(null);
        for (Exam exam : exams) {
            syncExamStatusIfNeeded(exam);
        }

        long notStarted = exams.stream().filter(e -> "NOT_STARTED".equals(e.getStatus())).count();
        long ongoing = exams.stream().filter(e -> "ONGOING".equals(e.getStatus())).count();
        long finished = exams.stream().filter(e -> "FINISHED".equals(e.getStatus())).count();

        long totalSubmission = examRecordMapper.selectCount(null);
        long pendingManual = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getStatus, "MARKING"));

        return new TeacherTodoResponse(notStarted, ongoing, finished, totalSubmission, pendingManual);
    }

    @Transactional(rollbackFor = Exception.class)
    public Object createParallelGroup(CreateParallelGroupRequest request) {
        if (request.paperIds() == null || request.paperIds().size() < 2) {
            throw new IllegalArgumentException("至少需要两套试卷");
        }

        for (Long paperId : request.paperIds()) {
            if (paperMapper.selectById(paperId) == null) {
                throw new IllegalArgumentException("试卷不存在: " + paperId);
            }
        }

        ParallelGroup group = new ParallelGroup();
        group.setName(request.name());
        group.setPaperIds(request.paperIds());
        group.setCreateTime(LocalDateTime.now());
        parallelGroupMapper.insert(group);

        return group;
    }

    public List<Object> listParallelGroups() {
        return new ArrayList<Object>(parallelGroupMapper.selectList(new LambdaQueryWrapper<ParallelGroup>().orderByDesc(ParallelGroup::getCreateTime)));
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Object> assignParallelPapers(Long groupId, AssignParallelRequest request) {
        ParallelGroup group = parallelGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("平行组不存在");
        }
        if (group.getPaperIds() == null || group.getPaperIds().isEmpty()) {
            throw new IllegalArgumentException("平行组未配置试卷");
        }

        List<Object> result = new ArrayList<>();
        for (String studentName : request.studentNames()) {
            Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getName, studentName));
            if (student == null) {
                continue;
            }
            Long paperId = group.getPaperIds().get((int) (student.getId() % group.getPaperIds().size()));

            LambdaQueryWrapper<ParallelAssignment> query = new LambdaQueryWrapper<ParallelAssignment>()
                    .eq(ParallelAssignment::getGroupId, groupId)
                    .eq(ParallelAssignment::getStudentId, student.getId());
            ParallelAssignment assignment = parallelAssignmentMapper.selectOne(query);
            if (assignment == null) {
                assignment = new ParallelAssignment();
                assignment.setGroupId(groupId);
                assignment.setStudentId(student.getId());
                assignment.setCreateTime(LocalDateTime.now());
            }
            assignment.setStudentName(student.getName());
            assignment.setPaperId(paperId);

            if (assignment.getId() == null) {
                parallelAssignmentMapper.insert(assignment);
            } else {
                parallelAssignmentMapper.updateById(assignment);
            }
            result.add(assignment);
        }
        return result;
    }

    public List<Object> listAssignments() {
        return new ArrayList<Object>(parallelAssignmentMapper.selectList(
                new LambdaQueryWrapper<ParallelAssignment>().orderByDesc(ParallelAssignment::getCreateTime)
        ));
    }

    public String exportExamSubmissionsCsv(Long examId) {
        return "";
    }

    private void syncExamStatusIfNeeded(Exam exam) {
        String realStatus = calcStatus(exam.getStartTime(), exam.getEndTime());
        if (!Objects.equals(realStatus, exam.getStatus())) {
            exam.setStatus(realStatus);
            examMapper.updateById(exam);
        }
    }

    private String calcStatus(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (start != null && now.isBefore(start)) {
            return "NOT_STARTED";
        }
        if (end != null && now.isAfter(end)) {
            return "FINISHED";
        }
        return "ONGOING";
    }

    private Long pickAssignedPaperId(String paperIdsStr, Long studentId) {
        if (paperIdsStr == null || paperIdsStr.isBlank()) {
            throw new IllegalArgumentException("考试未绑定试卷");
        }
        String[] pIds = paperIdsStr.split(",");
        int idx = Math.floorMod(studentId.intValue(), pIds.length);
        return Long.valueOf(pIds[idx]);
    }

    private boolean isObjectiveCorrect(String type, String standardAnswer, String userAnswer) {
        if (standardAnswer == null) {
            return false;
        }
        if ("MULTIPLE_CHOICE".equals(type)) {
            return normalizeMultipleAnswer(standardAnswer).equals(normalizeMultipleAnswer(userAnswer));
        }
        return standardAnswer.trim().equalsIgnoreCase(userAnswer == null ? "" : userAnswer.trim());
    }

    private String normalizeAnswerByType(String type, String rawAnswer) {
        if ("MULTIPLE_CHOICE".equals(type)) {
            return normalizeMultipleAnswer(rawAnswer);
        }
        return rawAnswer == null ? "" : rawAnswer.trim();
    }

    private String normalizeMultipleAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }
        return Arrays.stream(answer.toUpperCase().replaceAll("\\s+", "").split(","))
                .filter(x -> !x.isBlank())
                .map(x -> x.substring(0, 1))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }
}
