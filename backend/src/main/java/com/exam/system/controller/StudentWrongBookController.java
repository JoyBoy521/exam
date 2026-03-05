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
        wb.setCreateTime(LocalDateTime.now());
        studentWrongBookMapper.insert(wb);
        return "已成功加入错题本";
    }

    @GetMapping("/list")
    public List<Map<String, Object>> getMyWrongBook(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        List<StudentWrongBook> list = studentWrongBookMapper.selectList(
                new LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .orderByDesc(StudentWrongBook::getCreateTime)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentWrongBook wb : list) {
            Question q = questionMapper.selectById(wb.getQuestionId());
            if (q == null) {
                continue;
            }
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
            m.put("createTime", wb.getCreateTime());
            result.add(m);
        }
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
}
