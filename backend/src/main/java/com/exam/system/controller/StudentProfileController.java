package com.exam.system.controller;

import com.exam.system.dto.ChangePasswordRequest;
import com.exam.system.entity.ClassInfo;
import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.Student;
import com.exam.system.entity.StudentWrongBook;
import com.exam.system.mapper.ClassInfoMapper;
import com.exam.system.mapper.ExamRecordMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.mapper.StudentWrongBookMapper;
import com.exam.system.service.AuthService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    private final AuthService authService;
    private final StudentMapper studentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ExamRecordMapper examRecordMapper;
    private final StudentWrongBookMapper studentWrongBookMapper;

    public StudentProfileController(AuthService authService,
                                    StudentMapper studentMapper,
                                    ClassInfoMapper classInfoMapper,
                                    ExamRecordMapper examRecordMapper,
                                    StudentWrongBookMapper studentWrongBookMapper) {
        this.authService = authService;
        this.studentMapper = studentMapper;
        this.classInfoMapper = classInfoMapper;
        this.examRecordMapper = examRecordMapper;
        this.studentWrongBookMapper = studentWrongBookMapper;
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                 HttpServletRequest httpRequest) {
        Long studentId = CurrentUser.userId(httpRequest);
        authService.changeStudentPassword(studentId, request.oldPassword(), request.newPassword());
        return "密码修改成功";
    }

    @GetMapping("/info")
    public Map<String, Object> info(HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        ClassInfo cls = student.getClassId() == null ? null : classInfoMapper.selectById(student.getClassId());
        long recordCount = examRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, studentId)
        );
        long wrongCount = studentWrongBookMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
        );
        long masteredCount = studentWrongBookMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentWrongBook>()
                        .eq(StudentWrongBook::getStudentId, studentId)
                        .eq(StudentWrongBook::getMasteryLevel, "MASTERED")
        );

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", student.getId());
        m.put("studentNo", student.getStudentNo());
        m.put("name", student.getName());
        m.put("phone", student.getPhone());
        m.put("classId", student.getClassId());
        m.put("className", cls == null ? "-" : cls.getName());
        m.put("createTime", student.getCreateTime());
        m.put("recordCount", recordCount);
        m.put("wrongCount", wrongCount);
        m.put("masteredCount", masteredCount);
        return m;
    }

    @PostMapping("/update-basic")
    public String updateBasic(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long studentId = CurrentUser.userId(request);
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        String name = payload.get("name") == null ? null : String.valueOf(payload.get("name")).trim();
        String phone = payload.get("phone") == null ? null : String.valueOf(payload.get("phone")).trim();
        if (name != null && !name.isBlank()) {
            student.setName(name);
        }
        student.setPhone((phone == null || phone.isBlank()) ? null : phone);
        studentMapper.updateById(student);
        return "资料更新成功";
    }
}
