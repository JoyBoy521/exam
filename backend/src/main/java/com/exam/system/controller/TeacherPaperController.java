package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.CreateRandomPaperRequest;
import com.exam.system.entity.Paper;
import com.exam.system.entity.PaperQuestion;
import com.exam.system.entity.Question;
import com.exam.system.mapper.PaperMapper;
import com.exam.system.mapper.PaperQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/papers")
public class TeacherPaperController {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;

    public TeacherPaperController(PaperMapper paperMapper, PaperQuestionMapper paperQuestionMapper, QuestionMapper questionMapper) {
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
    }

    @GetMapping
    public List<Map<String, Object>> getPapers() {
        List<Paper> papers = paperMapper.selectList(new LambdaQueryWrapper<Paper>().orderByDesc(Paper::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Paper p : papers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("createTime", p.getCreateTime());
            map.put("totalScore", p.getTotalScore());

            List<Long> qIds = paperQuestionMapper.selectList(
                            new LambdaQueryWrapper<PaperQuestion>()
                                    .eq(PaperQuestion::getPaperId, p.getId())
                                    .orderByAsc(PaperQuestion::getSortOrder, PaperQuestion::getId)
                    ).stream()
                    .map(PaperQuestion::getQuestionId)
                    .collect(Collectors.toList());

            map.put("questionIds", qIds);
            result.add(map);
        }
        return result;
    }

    @PostMapping("/manual")
    @Transactional(rollbackFor = Exception.class)
    public Long createManualPaper(@RequestBody Map<String, Object> payload) {
        String title = String.valueOf(payload.getOrDefault("title", "未命名手动试卷"));
        List<?> idsRaw = (List<?>) payload.get("questionIds");
        if (idsRaw == null || idsRaw.isEmpty()) {
            throw new IllegalArgumentException("手动组卷至少包含一道题");
        }

        List<Long> questionIds = idsRaw.stream().map(x -> Long.valueOf(String.valueOf(x))).toList();

        Paper paper = new Paper();
        paper.setTitle(title);
        paper.setCreateBy("teacher");
        paper.setCreateTime(LocalDateTime.now());
        paperMapper.insert(paper);

        BigDecimal totalScore = BigDecimal.ZERO;
        int sort = 1;
        for (Long qId : questionIds) {
            if (questionMapper.selectById(qId) == null) {
                continue;
            }
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(qId);
            pq.setSortOrder(sort++);
            pq.setScore(BigDecimal.valueOf(5));
            totalScore = totalScore.add(pq.getScore());
            paperQuestionMapper.insert(pq);
        }

        paper.setTotalScore(totalScore);
        paperMapper.updateById(paper);
        return paper.getId();
    }

    @PostMapping("/random")
    @Transactional(rollbackFor = Exception.class)
    public Long createRandomPaper(@RequestBody CreateRandomPaperRequest request) {
        if (request.getRules() == null || request.getRules().isEmpty()) {
            throw new IllegalArgumentException("抽题规则不能为空");
        }

        Paper paper = new Paper();
        paper.setTitle(request.getTitle());
        paper.setCreateBy("teacher");
        paper.setCreateTime(LocalDateTime.now());
        paperMapper.insert(paper);

        int order = 1;
        int totalScore = 0;
        List<PaperQuestion> paperQuestions = new ArrayList<>();

        for (CreateRandomPaperRequest.RandomRuleItem rule : request.getRules()) {
            int count = rule.getCount() == null ? 0 : rule.getCount();
            if (count <= 0) {
                continue;
            }
            int scorePerItem = rule.getScorePerItem() == null ? 1 : rule.getScorePerItem();

            List<Long> selectedIds = getRandomQuestions(rule.getQuestionType(), rule.getDifficulty(), count);
            if (selectedIds.size() < count) {
                throw new IllegalArgumentException("题库数量不足！题型: " + rule.getQuestionType() + " 难度: " + rule.getDifficulty() + " 仅存 " + selectedIds.size() + " 题");
            }

            for (Long qId : selectedIds) {
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(qId);
                pq.setScore(BigDecimal.valueOf(scorePerItem));
                pq.setSortOrder(order++);
                paperQuestions.add(pq);
                totalScore += scorePerItem;
            }
        }

        if (paperQuestions.isEmpty()) {
            throw new IllegalArgumentException("未生成任何题目，请检查抽题规则");
        }

        for (PaperQuestion pq : paperQuestions) {
            paperQuestionMapper.insert(pq);
        }

        paper.setTotalScore(BigDecimal.valueOf(totalScore));
        paperMapper.updateById(paper);
        return paper.getId();
    }

    private List<Long> getRandomQuestions(String type, Integer difficulty, int count) {
        if (count <= 0) {
            return List.of();
        }
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            query.eq(Question::getType, type);
        }
        if (difficulty != null) {
            query.eq(Question::getDifficulty, difficulty);
        }
        List<Question> list = questionMapper.selectList(query);
        Collections.shuffle(list);
        return list.stream().limit(Math.min(count, list.size())).map(Question::getId).toList();
    }
}
