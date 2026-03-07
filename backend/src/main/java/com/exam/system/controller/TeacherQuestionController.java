package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Question;
import com.exam.system.mapper.QuestionMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/questions")
public class TeacherQuestionController {

    private final QuestionMapper questionMapper;

    public TeacherQuestionController(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    // 1. 获取题库列表 (支持按题型和关键词搜索)
    @GetMapping
    public List<Question> getQuestions(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer difficulty) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            query.eq(Question::getType, type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(Question::getStem, keyword);
        }
        if (difficulty != null) {
            query.eq(Question::getDifficulty, difficulty);
        }
        query.orderByDesc(Question::getId);
        return questionMapper.selectList(query);
    }

    // 2. 新增题目
    @PostMapping
    public void addQuestion(@RequestBody Map<String, Object> payload) {
        Question q = new Question();
        q.setType((String) payload.get("type"));
        q.setStem((String) payload.get("stem"));
        q.setAnswer((String) payload.get("answer"));
        q.setDifficulty(2); // 默认中等难度
        q.setCreateTime(LocalDateTime.now());

        // 因为实体类里加了 JacksonTypeHandler，所以这里直接传 List 即可，不需要转成 JSON 字符串！
        if (payload.get("options") != null) {
            q.setOptions((List<String>) payload.get("options"));
        }
        if (payload.get("knowledgePoints") != null) {
            q.setKnowledgePoints((List<String>) payload.get("knowledgePoints"));
        }
        questionMapper.insert(q);
    }

    // 3. 修改题目
    @PutMapping("/{id}")
    public void updateQuestion(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Question q = questionMapper.selectById(id);
        if (q == null) return;

        if (payload.containsKey("type")) q.setType((String) payload.get("type"));
        if (payload.containsKey("stem")) q.setStem((String) payload.get("stem"));
        if (payload.containsKey("answer")) q.setAnswer((String) payload.get("answer"));

        // 同样直接传 List
        if (payload.containsKey("options")) {
            q.setOptions((List<String>) payload.get("options"));
        }
        if (payload.containsKey("knowledgePoints")) {
            q.setKnowledgePoints((List<String>) payload.get("knowledgePoints"));
        }
        questionMapper.updateById(q);
    }

    // 4. 删除题目
    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questionMapper.deleteById(id);
    }
}
